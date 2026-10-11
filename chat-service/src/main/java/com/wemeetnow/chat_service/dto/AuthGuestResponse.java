package com.wemeetnow.chat_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthGuestResponse {
    private Long userId;      // 사용자 고유 ID
    private String username;  // 사용자 이름
    private String email;
    private String uuId;
    private String guestToken;  // 만료되어 갱신된 경우 새로 발급된 토큰(갱신이 없었다면 기존 토큰)
    private String isRefreshed; // 토큰 갱신 여부 (Y/N)
}
