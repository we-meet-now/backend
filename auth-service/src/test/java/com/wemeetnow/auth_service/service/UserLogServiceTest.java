package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.UserLog;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserLogServiceTest {

    @Test
    void savesLoginMetadataAndClassifiesMobileDevice() {
        UserLogPersistenceService persistenceService = mock(UserLogPersistenceService.class);
        UserLogService service = new UserLogService(persistenceService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/v1/users/login");
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "203.0.113.9, 10.0.0.2");
        request.addHeader("X-Request-ID", "request-123");
        request.addHeader("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)");

        service.recordUserLog(request, "42", "/api/auth/v1/users/login", 202, 37L);

        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        UserLog saved = captor.getValue();
        assertThat(saved.getInpUserId()).isEqualTo("42");
        assertThat(saved.getReqMethod()).isEqualTo("POST");
        assertThat(saved.getMenuLink()).isEqualTo("/api/auth/v1/users/login");
        assertThat(saved.getUserIp()).isEqualTo("203.0.113.9");
        assertThat(saved.getResponseStatus()).isEqualTo(202);
        assertThat(saved.getRequestId()).isEqualTo("request-123");
        assertThat(saved.getDurationMs()).isEqualTo(37L);
        assertThat(saved.getRequestUri()).isEqualTo("/api/auth/v1/users/login");
        assertThat(saved.getDeviceType()).isEqualTo("MOBILE");
    }

    @Test
    void classifiesAndroidTabletAsTablet() {
        UserLogPersistenceService persistenceService = mock(UserLogPersistenceService.class);
        UserLogService service = new UserLogService(persistenceService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/v1/users/login");
        request.addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 13; SM-X700) AppleWebKit/537.36");

        service.recordUserLog(request, "42", "/api/auth/v1/users/login", 500, 12L);

        ArgumentCaptor<UserLog> captor = ArgumentCaptor.forClass(UserLog.class);
        verify(persistenceService).insert(captor.capture());
        assertThat(captor.getValue().getDeviceType()).isEqualTo("TABLET");
    }
}
