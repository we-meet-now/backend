package com.wemeetnow.apigateway_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 전역 CORS 설정
 * - 프론트엔드(Vercel)에서 API Gateway로 직접 호출 시 발생하는 CORS 에러 방지
 * - front.url 프로퍼티로 허용 Origin을 관리 (환경별 application.properties에서 설정)
 * - 쉼표로 구분된 복수 Origin 지원 (예: https://front-azure-iota.vercel.app,http://localhost:5173)
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${front.url}")
    private String frontUrl;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 쉼표 구분으로 복수 Origin 허용
        String[] allowedOrigins = frontUrl.split(",");
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
