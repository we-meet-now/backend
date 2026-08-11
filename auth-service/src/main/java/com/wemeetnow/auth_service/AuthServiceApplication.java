package com.wemeetnow.auth_service;

import com.wemeetnow.auth_service.config.auth.PrincipalDetails;
import com.wemeetnow.auth_service.config.common.AuditorAwareImpl;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.TimeZone;
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
	@PostConstruct
	public void setKstTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
	}

	public static void main(String[] args) {
		// 1. 프로파일 파악 (System Property -> OS Environment -> Default 'local')
		String activeProfile = System.getProperty("spring.profiles.active");
		if (activeProfile == null || activeProfile.isBlank()) {
			activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
		}
		if (activeProfile == null || activeProfile.isBlank()) {
			activeProfile = "local"; // IDE에서 직접 메인 클래스 실행 시 적용
		}
		// 2. 해당 프로파일의 .env 파일 로드 (.env.local, .env.dev, .env.prod)
		Dotenv dotenv = Dotenv.configure()
				.filename(".env." + activeProfile)
				.ignoreIfMissing() // 배포 서버에서 .env 파일 대신 OS 환경변수를 쓸 경우 대비
				.load();

		// 3. 로드한 .env 항목을 System Property로 주입
		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		SpringApplication.run(AuthServiceApplication.class, args);
	}

}
