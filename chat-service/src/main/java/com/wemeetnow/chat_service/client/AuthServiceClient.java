package com.wemeetnow.chat_service.client;

import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.dto.ChatParticipantPublicProfileDto;
import com.wemeetnow.chat_service.dto.AuthFriendInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "auth-service", url = "${auth-service.url}")
public interface AuthServiceClient {
    @GetMapping("/api/auth/v1/users/userId={userId}")
    CommonApiResponse<ChatParticipantPublicProfileDto> getUserById(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("userId") Long userId);

    @GetMapping("/api/auth/v1/friends")
    CommonApiResponse<List<AuthFriendInfoDto>> getFriendList(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader);
}