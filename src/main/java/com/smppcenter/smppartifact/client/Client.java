package com.smppcenter.smppartifact.client;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.smppcenter.smppartifact.constant.BindType;
import com.smppcenter.smppartifact.util.ElinkTask;
import com.smppcenter.smppartifact.util.RebindTask;

@Service
@Scope("prototype")
public class Client implements Runnable{
	public static final Logger log = LoggerFactory.getLogger(Client.class);

	public SmppSessionConfiguration cfg;
	protected SmppSessionHandler sessionHandler;
	protected ClientState state;
	protected volatile SmppSession session;
	protected SmppClient smppClient;
	protected ScheduledExecutorService timer;
	
	protected ScheduledFuture<?> elinkTask;
	protected ScheduledFuture<?> rebindTask;
	
	protected long rebindPeriod = 5;
	protected long elinkPeriod = 5;
	
	
	private static void log(WindowFuture<Integer, PduRequest, PduResponse> future) {
		SubmitSm req = (SubmitSm) future.getRequest();
		SubmitSmResp resp = (SubmitSmResp) future.getResponse();

		log.debug("Resposta recebida com MSG ID={} para APPID={}", resp.getMessageId(), req.getReferenceObject());
	}
	
	@Override
	public void run() {
		System.out.println("Creating client");
	}

	public Client(SmppSessionConfiguration cfg) {
		this.cfg = cfg;
		
		this.timer = Executors.newScheduledThreadPool(2);
	}
	
	public void start() {
		log.debug("Starting client");

		this.smppClient = new DefaultSmppClient();

		this.bind();
	}
	
	private SmppSessionConfiguration createSessionConfiguration(String host, int porta, String systemId,
			String password) {
		SmppSessionConfiguration sessionCfg = new SmppSessionConfiguration();

		sessionCfg.setType(SmppBindType.TRANSCEIVER);
		sessionCfg.setHost(host);
		sessionCfg.setPort(porta);
		sessionCfg.setSystemId(systemId);
		sessionCfg.setPassword(password);

		return sessionCfg;

	}

	public SubmitSm sendSM(String host, int porta, String systemId, String password, String origem, String destino,
			String msg, String dataCode) throws SmppInvalidArgumentException {
		SubmitSm sm = new SubmitSm();

		sm.setSourceAddress(new Address((byte) 5, (byte) 0, origem));
		sm.setDestAddress(new Address((byte) 1, (byte) 1, destino));
		sm.setShortMessage(CharsetUtil.encode(msg, dataCode));
		sm.setRegisteredDelivery((byte) 1);
		sm.setDataCoding((byte) 8);

		try {
			System.out.println(this.getSession());
			this.getSession().submit(sm, TimeUnit.SECONDS.toMillis(60));
		} catch (RecoverablePduException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		} catch (UnrecoverablePduException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SmppTimeoutException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SmppChannelException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		}
		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException ex) {
			java.util.logging.Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
		}

		return sm;
	}

	public void bind()  {
		this.state = ClientState.BINDING;
		System.out.println(this.getSession());
		this.bound(this.session);
		runRebindTask();
	}
	
	public void bound(SmppSession session) {
		this.state = ClientState.BOUND;
		this.session = session;		
		
		if (rebindTask!=null) {
			this.rebindTask.cancel(true);
		}
		runElinkTask();
	}

	private SmppBindType getBindType(BindType type) {
		switch (type) {
		case TRANSCEIVER:
			return SmppBindType.TRANSCEIVER;
		case RECEIVER:
			return SmppBindType.RECEIVER;
		case TRANSMITTER:
			return SmppBindType.TRANSMITTER;
		default:
			return SmppBindType.TRANSCEIVER;
		}
	}

	public void stop() {
		System.out.println("Stopping !!!");
		this.state = ClientState.STOPPING;
		this.stop();
		this.elinkTask.cancel(true);
		this.rebindTask.cancel(true);
		this.timer.shutdown();
		this.timer = null;
	}

	private void runRebindTask() {
		this.rebindTask = this.timer.scheduleAtFixedRate(new RebindTask(this), 0, getRebindPeriod(), 
				TimeUnit.SECONDS);
	}

	private void runElinkTask() {
		this.elinkTask = this.timer.scheduleAtFixedRate(new ElinkTask(this), getElinkPeriod(), getElinkPeriod(),
				TimeUnit.SECONDS);
	}

	public SmppSessionConfiguration getCfg() {
		return cfg;
	}

	public void setCfg(SmppSessionConfiguration cfg) {
		this.cfg = cfg;
	}

	public SmppSessionHandler getSessionHandler() {
		return sessionHandler;
	}

	public void setSessionHandler(SmppSessionHandler sessionHandler) {
		this.sessionHandler = sessionHandler;
	}

	public SmppSession getSession() {
		return session;
	}

	public void setSession(SmppSession session) {
		this.session = session;
	}

	public SmppClient getSmppClient() {
		return smppClient;
	}

	public void setSmppClient(SmppClient smppClient) {
		this.smppClient = smppClient;
	}

	public long getRebindPeriod() {
		return rebindPeriod;
	}

	public void setRebindPeriod(long rebindPeriod) {
		this.rebindPeriod = rebindPeriod;
	}

	public long getElinkPeriod() {
		return elinkPeriod;
	}

	public void setElinkPeriod(long elinkPeriod) {
		this.elinkPeriod = elinkPeriod;
	}

	public ClientState getState() {
		return state;
	}

	public void setState(ClientState state) {
		this.state = state;
	}
}
