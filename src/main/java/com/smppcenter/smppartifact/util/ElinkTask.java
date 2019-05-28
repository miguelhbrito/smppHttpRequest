package com.smppcenter.smppartifact.util;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.smppcenter.smppartifact.client.Client;
import com.smppcenter.smppartifact.client.ClientState;

public class ElinkTask implements Runnable {
	private static final Logger log = LoggerFactory.getLogger(ElinkTask.class);

	SmppSession smppSession;
	Integer enquireLinkTimeout;
	private Client client;

	public ElinkTask(Client client) {
		this.client = client;
	}
	
	public void run() {
		if (client.getState() == ClientState.BOUND) {
			
			SmppSession session = client.getSession();

			log.debug("Enviando elink");

			try {
				session.enquireLink(new EnquireLink(), TimeUnit.SECONDS.toMillis(10));

				log.debug("Elink enviado com sucesso !!");
				
			} catch (RecoverablePduException ex) {
				log.debug("{}", ex);
			} catch (UnrecoverablePduException ex) {
				log.debug("{}", ex);
			} catch (SmppTimeoutException ex) {
				
				client.bind();
				log.debug("{}", ex);
			} catch (SmppChannelException ex) {
				log.debug("{}", ex);
			} catch (InterruptedException ex) {
				log.debug("{}", ex);
			}
		}

	}

}
