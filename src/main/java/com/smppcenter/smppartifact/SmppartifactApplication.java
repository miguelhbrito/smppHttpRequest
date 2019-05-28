package com.smppcenter.smppartifact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.smppcenter.smppartifact.client",
				"com.smppcenter.smppartifact.rest",
				"com.smppcenter.smppartifact.service"})
public class SmppartifactApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmppartifactApplication.class, args);
	}

}
