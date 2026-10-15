package com.wemeetnow.chat_service.config;

import com.wemeetnow.chat_service.config.jwt.JwtFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class PublicTableReadFilterTest {

    @Test
    void skipsJwtProcessingOnlyForFullTableGetRoutes() {
        ExposedJwtFilter filter = new ExposedJwtFilter();

        assertThat(filter.skips(request("GET", "/api/chat/v1/chats/all"))).isTrue();
        assertThat(filter.skips(request("GET", "/api/chat/v1/chat-rooms/all"))).isTrue();
        assertThat(filter.skips(request("GET", "/api/chat/v1/chat-participants/all"))).isTrue();
        assertThat(filter.skips(request("GET", "/api/chat/v1/chat-rooms/roomId=1"))).isFalse();
        assertThat(filter.skips(request("POST", "/api/chat/v1/chats/all"))).isFalse();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static class ExposedJwtFilter extends JwtFilter {
        boolean skips(HttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
