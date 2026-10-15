package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.config.jwt.JwtFilter;
import com.wemeetnow.auth_service.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PublicTableReadFilterTest {

    @Test
    void skipsJwtProcessingOnlyForFullTableGetRoutes() {
        ExposedJwtFilter filter = new ExposedJwtFilter();
        MockHttpServletRequest userRequest = request("GET", "/api/auth/v1/users/all");
        MockHttpServletRequest friendRequest = request("GET", "/api/auth/v1/friends/all");
        MockHttpServletRequest securedRequest = request("GET", "/api/auth/v1/users/get-user-info");
        MockHttpServletRequest postRequest = request("POST", "/api/auth/v1/users/all");

        assertThat(filter.skips(userRequest)).isTrue();
        assertThat(filter.skips(friendRequest)).isTrue();
        assertThat(filter.skips(securedRequest)).isFalse();
        assertThat(filter.skips(postRequest)).isFalse();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }

    private static class ExposedJwtFilter extends JwtFilter {
        private ExposedJwtFilter() {
            super(mock(UserService.class));
        }

        boolean skips(HttpServletRequest request) {
            return shouldNotFilter(request);
        }
    }
}
