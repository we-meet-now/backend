package com.wemeetnow.chat_service;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.TimeZone;

@EnableJpaAuditing // @CreatedDate 붙은 필드 인식
@SpringBootApplication
public class ChatServiceApplication {

	@PostConstruct
	public void setKstTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		SpringApplication.run(ChatServiceApplication.class, args);
	}

}
