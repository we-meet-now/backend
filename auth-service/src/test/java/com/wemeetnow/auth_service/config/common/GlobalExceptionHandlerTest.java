package com.wemeetnow.auth_service.config.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void mapsUnknownApiRoutesToNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.POST, "/api/auth/v1/users/no-login/removed"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getStatusCode()).isEqualTo("4040");
    }
}
