package com.olegchir.jug.site.parser.jbreak2018parser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class JBreak2018ParserApplication {

	public static void main(String[] args) {
		SpringApplication.run(JBreak2018ParserApplication.class, args);
	}

	@Bean
	public AppRunner appRunner() {
		return new AppRunner();
	}
}
