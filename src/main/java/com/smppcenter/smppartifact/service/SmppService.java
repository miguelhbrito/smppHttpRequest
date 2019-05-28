package com.smppcenter.smppartifact.service;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudhopper.commons.charset.CharsetUtil;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.smppcenter.smppartifact.client.Client;
import com.smppcenter.smppartifact.client.ClientState;
import com.smppcenter.smppartifact.client.SmppClientSessionHandler;

@Service
public class SmppService {
	
	public static final Logger log = LoggerFactory.getLogger(Client.class);
	
	public <T> void start(String host, int porta, String systemId, String password, String origem, String destino,
			String msg, String dataCode) throws SmppInvalidArgumentException, SmppBindException, SmppTimeoutException,
			SmppChannelException, UnrecoverablePduException, InterruptedException {
		
		SmppSessionConfiguration sessionCfg = new SmppSessionConfiguration();

		sessionCfg.setType(SmppBindType.TRANSCEIVER);
		sessionCfg.setHost(host);
		sessionCfg.setPort(porta);
		sessionCfg.setSystemId(systemId);
		sessionCfg.setPassword(password);
		
		Client client = new Client(sessionCfg);
		
		client.setSessionHandler(new SmppClientSessionHandler(client));
		
		ExecutorService pool = Executors.newFixedThreadPool(2);

		pool.submit(client);
		
		client.start();
		
		SmppClient smppClient = client.getSmppClient();
		
		SmppSession session = smppClient.bind(client.getCfg(), client.getSessionHandler());
		
		SubmitSm sm = new SubmitSm();
		
		client.setSession(session);

		sm = client.sendSM(host, porta, systemId, password, origem, destino, msg, dataCode);

//		client.stop();
	}
	
	public void stop () {
		Client client;
		
		
	}
}
