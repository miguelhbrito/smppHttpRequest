package com.smppcenter.smppartifact.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.smppcenter.smppartifact.client.Client;
import com.smppcenter.smppartifact.client.ClientState;

public class RebindTask implements Runnable{

	public static final Logger log = LoggerFactory.getLogger(RebindTask.class);
	protected Client client;
	public RebindTask(Client client) {
		this.client = client;
	}
	
	public void run() {
		if (client.getState() == ClientState.BINDING) {
			SmppClient smppClient = client.getSmppClient();
			try {
				log.debug("Tentando conectar(bind) !!!");

				SmppSession session = smppClient.bind(client.getCfg(), client.getSessionHandler());

				client.bound(session);
				
			} catch (SmppTimeoutException ex) {
				log.debug("{}", ex);
			} catch (SmppChannelException ex) {
				log.debug("{}", ex);
			} catch (SmppBindException ex) {
				log.debug("{}", ex);
			} catch (UnrecoverablePduException ex) {
				log.debug("{}", ex);
			} catch (InterruptedException ex) {
				log.debug("{}", ex);
			}
		}		
	}

}
