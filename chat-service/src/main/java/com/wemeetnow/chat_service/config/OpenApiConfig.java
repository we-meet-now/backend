package com.wemeetnow.chat_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {

        // 1. API 기본 정보 설정
        Info info = new Info()
                .title("WeMeetNow Chat Service API")
                .version("v1.0.0")
                .description("채팅 서비스의 API 명세서입니다.");



        // 4. OpenAPI Bean 생성
        return new OpenAPI()
                .info(info) // API 정보 설정
                ; // SecurityScheme 컴포넌트 추가
    }
}