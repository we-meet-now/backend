package com.wemeetnow.chat_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 전역 CORS 설정
 * - @CrossOrigin을 각 컨트롤러/메서드마다 붙이는 대신 이 설정 하나로 전체 적용
 * - front.url 프로퍼티로 허용 Origin을 관리 (환경별 application.properties에서 설정)
 * - 쉼표로 구분된 복수 Origin 지원 (예: https://app.vercel.app,http://localhost:5173)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${front.url}")
    private String frontUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 쉼표 구분으로 복수 Origin 허용 (e.g. "https://app.vercel.app,http://localhost:5173")
        String[] allowedOrigins = frontUrl.split(",");
        registry.addMapping("/api/**")          // REST API 전체 경로
                .allowedOrigins(allowedOrigins)  // front.url 프로퍼티 값 (환경별로 변경)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);                   // preflight 캐시 1시간
    }
}

