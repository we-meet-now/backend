package com.wemeetnow.chat_service.service;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.dto.ChatParticipantPublicProfileDto;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ChatServiceTest {

    private MockRestServiceServer mockServer;
    private ChatService chatService;
    private CommonService commonService;
    private AuthServiceClient authServiceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        commonService = new CommonService(restClientBuilder);
        ReflectionTestUtils.setField(commonService, "authServiceUrl", "http://auth-service");
        authServiceClient = mock(AuthServiceClient.class);
        chatService = new ChatService(
                mock(ChatRepository.class),
                mock(ChatRoomRepository.class),
                restClientBuilder,
                commonService,
                authServiceClient
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
                .hasMessage("Auth 서비스에서 사용자 정보를 받지 못했습니다.");
        mockServer.verify();
    }

    @Test
    void isValidGuestTokenResolvesUserIdFromTokenWithoutClientUuid() {
        mockServer.expect(requestTo("http://auth-service/api/auth/v1/users/get-guest-id"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer guest-token"))
                .andRespond(withSuccess(
                        """
                                {
                                  "statusCode": "2000",
                                  "data": {
                                    "userId": 42,
                                    "uuId": "guest-uuid",
                                    "statusCd": "2000",
                                    "statusMsg": "success",
                                    "isGuestYn": "Y",
                                    "guestToken": "guest-token",
                                    "isRefreshed": "N"
                                  },
                                  "message": "userId 조회 성공"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        var response = chatService.isValidGuestToken("guest-token");

        assertThat(response.getUserId()).isEqualTo(42L);
        assertThat(response.getUuId()).isEqualTo("guest-uuid");
        mockServer.verify();
    }

    @Test
    void fetchUserInfoFromAuthServiceReadsWrappedResponse() {
        mockServer.expect(requestTo("http://auth-service/api/auth/v1/users/get-user-info"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess(
                        """
                                {
                                  "statusCode": "2000",
                                  "data": {
                                    "userId": 6,
                                    "username": "test-user",
                                    "email": "test@example.com",
                                    "nickname": "tester"
                                  },
                                  "message": "사용자 정보 조회 성공"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        ChatUserInfo userInfo = commonService.fetchUserInfoFromAuthService("test-token");

        assertThat(userInfo.getUserId()).isEqualTo(6L);
        assertThat(userInfo.getUsername()).isEqualTo("test-user");
        mockServer.verify();
    }

    @Test
    void getChatResponseListLooksUpEachDistinctUserAndReturnsUsername() {
        List<Chat> chats = List.of(
                Chat.builder().chatId(1L).userId(8L).build(),
                Chat.builder().chatId(2L).userId(8L).build(),
                Chat.builder().chatId(3L).userId(9L).build()
        );
        when(authServiceClient.getUserById("Bearer access-token", 8L))
                .thenReturn(profile(8L, "first-author"));
        when(authServiceClient.getUserById("Bearer access-token", 9L))
                .thenReturn(profile(9L, null));

        var responses = chatService.getChatResponseList(chats, "access-token");

        assertThat(responses).extracting("username").containsExactly("first-author", "first-author", null);
        verify(authServiceClient).getUserById("Bearer access-token", 8L);
        verify(authServiceClient).getUserById("Bearer access-token", 9L);
    }

    @Test
    void getChatResponseListReturnsNullUsernameWhenAuthServiceHasNoUser() {
        List<Chat> chats = List.of(Chat.builder().chatId(1L).userId(404L).build());
        when(authServiceClient.getUserById("Bearer access-token", 404L))
                .thenThrow(mock(feign.FeignException.NotFound.class));

        var responses = chatService.getChatResponseList(chats, "Bearer access-token");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUsername()).isNull();
        verify(authServiceClient).getUserById("Bearer access-token", 404L);
    }

    private CommonApiResponse<ChatParticipantPublicProfileDto> profile(Long userId, String username) {
        return CommonApiResponse.<ChatParticipantPublicProfileDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(ChatParticipantPublicProfileDto.builder()
                        .userId(userId)
                        .username(username)
                        .build())
                .build();
    }
}
