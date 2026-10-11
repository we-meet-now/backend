package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.repository.ChatRepository;
import com.wemeetnow.chat_service.repository.ChatRoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatServiceTest {

    private MockRestServiceServer mockServer;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        chatService = new ChatService(
                mock(ChatRepository.class),
                mock(ChatRoomRepository.class),
                restClientBuilder
        );
        ReflectionTestUtils.setField(chatService, "authServiceUrl", "http://auth-service");
    }

    @Test
    void isValidAccessTokenReadsUserIdFromWrappedAuthResponse() {
        mockServer.expect(requestTo("http://auth-service/api/auth/v1/users/get-id"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(
                        """
                                {
                                  "statusCode": "2000",
                                  "data": {
                                    "userId": 6,
                                    "statusCd": "2000",
                                    "statusMsg": "success"
                                  },
                                  "message": "userId 조회 성공"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        AuthUserResponse response = chatService.isValidAccessToken("Bearer test-token");

        assertThat(response.getUserId()).isEqualTo(6L);
        mockServer.verify();
    }

    @Test
    void isValidAccessTokenRejectsResponseWithoutUserId() {
        mockServer.expect(requestTo("http://auth-service/api/auth/v1/users/get-id"))
                .andRespond(withSuccess(
                        "{\"statusCode\":\"2000\",\"data\":null,\"message\":\"success\"}",
                        MediaType.APPLICATION_JSON
                ));

        assertThatThrownBy(() -> chatService.isValidAccessToken("Bearer test-token"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Auth service did not return a user ID.");
        mockServer.verify();
    }
}
