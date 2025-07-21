package com.wemeetnow.auth_service;

import com.wemeetnow.auth_service.config.auth.PrincipalDetails;
import com.wemeetnow.auth_service.config.common.AuditorAwareImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
// import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
//@EnableDiscoveryClient
@EnableJpaAuditing(auditorAwareRef = "auditorAware") // @CreatedDate, @LastModifiedDate 적용위함, 생성자, 수정자 user_id값 자동 관리
public class AuthServiceApplication {
	@Autowired
	private PrincipalDetails principalDetails;
	@Bean
	public AuditorAware<String> auditorAware() {
		return new AuditorAwareImpl(principalDetails);
	}

	public static void main(String[] args) {
		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
