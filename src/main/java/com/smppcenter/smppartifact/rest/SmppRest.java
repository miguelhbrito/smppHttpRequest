package com.smppcenter.smppartifact.rest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.smppcenter.smppartifact.client.Client;
import com.smppcenter.smppartifact.client.SmppClientSessionHandler;
import com.smppcenter.smppartifact.service.SmppService;

@RestController
@RequestMapping("/")
public class SmppRest {
	public static final Logger log = LoggerFactory.getLogger(SmppRest.class);
//	protected SmppSessionConfiguration cfg;
	
	@RequestMapping(value = "send-sm", method = RequestMethod.GET)
	public <T> String sendSM(@RequestParam("host") String host,
						@RequestParam("porta") int porta,
						@RequestParam("systemId") String systemId,
						@RequestParam("password") String password,
						@RequestParam("origem") String origem, 
						@RequestParam("destino") String destino, 
						@RequestParam("msg") String msg, 
						@RequestParam("dataCode")String dataCode) throws Exception{
		log.debug("CHEGOU NO REST");
		SmppService smppservice = new SmppService();
		smppservice.start(host, porta, systemId, password, origem, destino, msg, dataCode);
		return "final rest";
	}
	
	@RequestMapping(value = "stop", method = RequestMethod.GET)
	public String stop(){
		log.debug("teste stop");
		SmppService smppservice = new SmppService();
		smppservice.stop();
		return "test stop feito !!!";
	}
	
	@RequestMapping(value = "teste", method = RequestMethod.GET)
	public String teste(){
		log.debug("Teste Rest !!!");
		System.out.println("Teste Rest !!!");
		return "teste rest feito !!!";
	}
	
	
}

