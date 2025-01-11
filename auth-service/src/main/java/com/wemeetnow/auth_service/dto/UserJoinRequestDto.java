package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class UserJoinRequestDto {
    private String email;
    private String password;
    private String passwordCorrect;
    private String nickname;
    private String username;
    private Role role;

    public User toEntity(String enCodedPassword) {
        return User.builder()
                .email(this.email)
                .password(enCodedPassword)
                .username(this.username)
                .nickname(this.nickname)
                .role(Role.ROLE_USER)
                .build();
    }
    public static UserJoinRequestDto fromKakaoDto(KakaoUserInfoResponseDto infoDto, Role kakaoUserRole) {
        return UserJoinRequestDto.builder()
                    .email(infoDto.getKakaoAccount().getEmail())
                    .password("Qwe123!!")
                    .passwordCorrect("Qwe123!!")
                    .nickname(infoDto.getKakaoAccount().getProfile().getNickName())
                    .username(infoDto.getKakaoAccount().name)
                    .role(kakaoUserRole)
                    .build();
    }
}