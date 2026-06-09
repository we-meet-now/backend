package com.wemeetnow.chat_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 전역 CORS 설정
 * - @CrossOrigin을 각 컨트롤러/메서드마다 붙이는 대신 이 설정 하나로 전체 적용
 * - front.url 프로퍼티로 허용 Origin을 관리 (환경별 application.properties에서 설정)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${front.url}")
    private String frontUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")          // REST API 전체 경로
                .allowedOrigins(frontUrl)        // front.url 프로퍼티 값 (환경별로 변경)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);                   // preflight 캐시 1시간
    }
}

