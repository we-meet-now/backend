package com.wemeetnow.chat_service.config;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Simple broker를 활성화하고 "/topic" prefix를 사용
        config.enableSimpleBroker("/topic");
        // 클라이언트에서 서버로 보내는 메시지의 prefix를 "/app"으로 설정
        config.setApplicationDestinationPrefixes("/app");
        System.out.println("WebSocketConfig.configureMessageBroker - 메시지 브로커 설정 완료");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        System.out.println("WebSocketConfig.registerStompEndpoints - STOMP 엔드포인트 등록");

        // SockJS를 사용하는 엔드포인트 등록
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // SockJS 없는 순수 WebSocket도 추가
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*");
    }
}