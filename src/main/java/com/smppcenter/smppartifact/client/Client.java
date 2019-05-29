package com.smppcenter.smppartifact.client;

import java.util.Random;
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
public class Client implements Runnable {
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
			String msg, String dataCode) throws Exception {

		byte[] textoBytes = CharsetUtil.encode(msg, CharsetUtil.CHARSET_UTF_8);

		int numMaximoPartesMensagem = 134;
		byte[] bytesMensagemUnica = textoBytes;
		byte[][] byteMensagensArray = splitUnicodeMessage(bytesMensagemUnica, numMaximoPartesMensagem);

		for (int i = 0; i < byteMensagensArray.length; i++) {
			SubmitSm sm = new SubmitSm();
			sm.setSourceAddress(new Address((byte) 5, (byte) 0, origem));
			sm.setDestAddress(new Address((byte) 1, (byte) 1, destino));
			sm.setShortMessage(byteMensagensArray[i]);
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

		}
		return null;
	}

	public void bind() {
		this.state = ClientState.BINDING;
		System.out.println(this.getSession());
		this.bound(this.session);
		runRebindTask();
	}

	public void bound(SmppSession session) {
		this.state = ClientState.BOUND;
		this.session = session;

		if (rebindTask != null) {
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

	private static byte[][] splitUnicodeMessage(byte[] binaryShortMessage, Integer numMaximoPartesMensagem) {

		if (binaryShortMessage == null) {
			return null;
		}

		// se a mensagem nao precisar ser concatenada
		if (binaryShortMessage.length <= 140) {
			return null;
		}

		// Field 1 (1 octet): Length of User Data Header, in this case 05.
		final byte UDHIE_HEADER_LENGTH = 0x05;
        // Field 2 (1 octet): Information Element Identifier, equal to 00 (Concatenated short messages, 8-bit reference number)
		final byte UDHIE_IDENTIFIER_SAR = 0x00;
        // Field 3 (1 octet): Length of the header, excluding the first two fields; equal to 03
		final byte UDHIE_SAR_LENGTH = 0x03;

		// determine how many messages have to be sent
		// since the UDH will be 6 bytes, we'll split the data into chunks of 134
		int numberOfSegments = binaryShortMessage.length / numMaximoPartesMensagem;
		int messageLength = binaryShortMessage.length;
		if (numberOfSegments > 255) {
			numberOfSegments = 255;
			// last part (only need to add remainder)
			messageLength = numberOfSegments * numMaximoPartesMensagem;
		}
		if ((messageLength % numMaximoPartesMensagem) > 0) {
			numberOfSegments++;
		}

		// prepare array for all of the msg segments
		byte[][] segments = new byte[numberOfSegments][];

		int lengthOfData;

		// generate new reference number
		byte[] referenceNumber = new byte[1];
		new Random().nextBytes(referenceNumber);

		// split the message adding required headers
		for (int i = 0; i < numberOfSegments; i++) {
			if (numberOfSegments - i == 1) {
				lengthOfData = messageLength - i * numMaximoPartesMensagem;
			} else {
				lengthOfData = numMaximoPartesMensagem;
			}
			// new array to store the header
			// part will be UDH (6 bytes) + length of part
			segments[i] = new byte[6 + lengthOfData];

			// UDH header
			// doesn't include itself, its header length
			segments[i][0] = UDHIE_HEADER_LENGTH;
			// SAR identifier
			segments[i][1] = UDHIE_IDENTIFIER_SAR;
			// SAR length
			segments[i][2] = UDHIE_SAR_LENGTH;
			// reference number (same for all messages)
            // Field 4 (1 octet): 00-FF, CSMS reference number, must be same for all the SMS parts in the CSMS
			segments[i][3] = referenceNumber[0];
			// total number of segments
            // Field 5 (1 octet): 00-FF, total number of parts. The value shall remain constant for every short message which makes up the concatenated short message. If the value is zero then the receiving entity shall ignore the whole information element
			segments[i][4] = (byte) numberOfSegments;
			// segment number
            // Field 6 (1 octet): 00-FF, this part's number in the sequence. The value shall start at 1 and increment for every short message which makes up the concatenated short message. If the value is zero or greater than the value in Field 5 then the receiving entity shall ignore the whole information element. [ETSI Specification: GSM 03.40 Version 5.3.0: July 1996]
			segments[i][5] = (byte) (i + 1);
			
			
			// copy the data into the array
            // copy this part's user data onto the end
			System.arraycopy(binaryShortMessage, (i * numMaximoPartesMensagem), segments[i], 6, lengthOfData);
		}
		return segments;
	}

	private void runRebindTask() {
		this.rebindTask = this.timer.scheduleAtFixedRate(new RebindTask(this), 0, getRebindPeriod(), TimeUnit.SECONDS);
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
