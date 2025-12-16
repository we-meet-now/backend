package com.wemeetnow.chat_service.client;

import com.wemeetnow.chat_service.dto.AuthUserDto;
import com.wemeetnow.chat_service.dto.ChatParticipantUserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.client.RestClient;

@FeignClient(name = "auth-service", url = "${auth-service.url}")
public interface AuthServiceClient {
    @GetMapping("/api/v1/users/userId={userId}")
    ChatParticipantUserDto getUserById(@PathVariable("userId") Long userId);
}