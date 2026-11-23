package com.wemeetnow.chat_service.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GuestBrowserSessionHandshakeInterceptorTest {
    private final GuestBrowserSessionHandshakeInterceptor interceptor =
            new GuestBrowserSessionHandshakeInterceptor();

    @Test
    void copiesGuestSessionCookieToHandshakeAttributes() {
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader(HttpHeaders.COOKIE, "other=value; guest_session=opaque-session-id; another=value");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(accepted).isTrue();
        assertThat(attributes)
                .containsEntry(
                        GuestBrowserSessionHandshakeInterceptor.SESSION_ID_ATTRIBUTE,
                        "opaque-session-id"
                );
    }

    @Test
    void doesNotAddAttributeWhenCookieIsAbsent() {
        Map<String, Object> attributes = new HashMap<>();
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                new ServletServerHttpResponse(new MockHttpServletResponse()),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(attributes).doesNotContainKey(
                GuestBrowserSessionHandshakeInterceptor.SESSION_ID_ATTRIBUTE
        );
    }
}
