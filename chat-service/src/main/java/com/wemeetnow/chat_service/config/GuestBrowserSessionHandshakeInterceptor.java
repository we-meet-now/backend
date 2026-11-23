package com.wemeetnow.chat_service.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class GuestBrowserSessionHandshakeInterceptor implements HandshakeInterceptor {
    public static final String SESSION_ID_ATTRIBUTE = "guestBrowserSessionId";
    private static final String COOKIE_NAME = "guest_session";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String sessionId = findCookie(request.getHeaders().get(HttpHeaders.COOKIE));
        if (sessionId != null) {
            attributes.put(SESSION_ID_ATTRIBUTE, sessionId);
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private String findCookie(Iterable<String> cookieHeaders) {
        if (cookieHeaders == null) {
            return null;
        }
        for (String cookieHeader : cookieHeaders) {
            for (String cookie : cookieHeader.split(";")) {
                int separator = cookie.indexOf('=');
                if (separator <= 0 || !COOKIE_NAME.equals(cookie.substring(0, separator).trim())) {
                    continue;
                }
                String value = cookie.substring(separator + 1).trim();
                if (!value.isEmpty()) {
                    return value;
                }
            }
        }
        return null;
    }
}
