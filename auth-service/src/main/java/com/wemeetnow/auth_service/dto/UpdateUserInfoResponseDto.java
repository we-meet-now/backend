package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInfoResponseDto {
    private Long userId;
    private String username;
    private String email;
    private String nickname;
    private String phoneNumber;
    private String imgUrl;
    private LocalDateTime mdfyDate;

    public static UpdateUserInfoResponseDto fromEntity(User user) {
        return UpdateUserInfoResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .phoneNumber(user.getPhoneNumber())
                .imgUrl(user.getImgUrl())
                .mdfyDate(user.getMdfyDate())
                .build();
    }
}
