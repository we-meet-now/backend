package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.repository.ChatParticipantRepository;
import com.wemeetnow.chat_service.repository.ChatReadRepository;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatRoomServiceGuestSessionTest {

    @Test
    void requestsNewGuestSessionAndReturnsItsRefreshToken() {
        var restClientBuilder = org.springframework.web.client.RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ChatRoomService chatRoomService = new ChatRoomService(
                mock(ChatRoomRepository.class),
                mock(ChatRepository.class),
                mock(ChatReadRepository.class),
                restClientBuilder,
                mock(ChatParticipantRepository.class),
                mock(EnterCodeGenerator.class),
                mock(AuthServiceClient.class)
        );
        ReflectionTestUtils.setField(chatRoomService, "AUTH_SERVICE_URL", "http://auth-service");

        server.expect(requestTo("http://auth-service/api/auth/v1/users/no-login/guest-sessions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"statusCode\":\"2000\",\"data\":{\"uuId\":\"guest-uuid\",\"guestToken\":\"guest-access-token\",\"refreshToken\":\"opaque-refresh-secret\",\"guestTokenExpiresInSeconds\":1800},\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        ChatGuestTokenInfo response = chatRoomService.generateGuestSession();

        assertThat(response.getUuId()).isEqualTo("guest-uuid");
        assertThat(response.getGuestToken()).isEqualTo("guest-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("opaque-refresh-secret");
        server.verify();
    }
}
