package com.smppcenter.smppartifact.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;

public class SmppClientSessionHandler extends DefaultSmppSessionHandler {
	public static Logger log = LoggerFactory.getLogger(SmppClientSessionHandler.class);
	protected Client client;

	public SmppClientSessionHandler(Client client) {
		this.client = client;
	}

	public PduResponse firePduRequestReceived(PduRequest pduRequest) {
		if (pduRequest.isRequest() && pduRequest.getClass() == DeliverSm.class) {
			log.debug("Recebeu DELIVER_SM");

			DeliverSm dlr = (DeliverSm) pduRequest;

			log.debug("Msg id={}", dlr.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID));
			log.debug("Status={}", dlr.getOptionalParameter(SmppConstants.TAG_MSG_STATE));

			return pduRequest.createResponse();
		}

		return super.firePduRequestReceived(pduRequest);
	}

	@Override
	public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
		if (pduAsyncResponse.getResponse().getClass() == SubmitSmResp.class) {
			SubmitSm req = (SubmitSm) pduAsyncResponse.getRequest();
			log.debug("Recebeu repostar para APPID={}", req.getReferenceObject());

			SubmitSmResp ssmr = (SubmitSmResp) pduAsyncResponse.getResponse();

			log.debug("Recebeu resposta com  MSG ID={} for seqnum={}", ssmr.getMessageId(), ssmr.getSequenceNumber());
		}
	}

	@Override
	public void fireChannelUnexpectedlyClosed() {
		client.bind();
	}
}
