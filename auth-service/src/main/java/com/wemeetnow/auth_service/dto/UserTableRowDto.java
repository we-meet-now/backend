package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserTableRowDto {
    private Long userId;
    private String username;
    private String email;
    private String password;
    private String nickname;
    private String provider;
    private Boolean emailAuth;
    private String imgUrl;
    private Role role;
    private String phoneNumber;
    private String postCd1;
    private String addr1;
    private String detailAddr1;
    private String uuid;
    private LocalDateTime inpDate;
    private LocalDateTime mdfyDate;
    private String inpUserId;
    private String mdfyUserId;

    public static UserTableRowDto fromEntity(User user) {
        return UserTableRowDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .password(user.getPassword())
                .nickname(user.getNickname())
                .provider(user.getProvider())
                .emailAuth(user.getEmailAuth())
                .imgUrl(user.getImgUrl())
                .role(user.getRole())
                .phoneNumber(user.getPhoneNumber())
                .postCd1(user.getPostCd1())
                .addr1(user.getAddr1())
                .detailAddr1(user.getDetailAddr1())
                .uuid(user.getUuid())
                .inpDate(user.getInpDate())
                .mdfyDate(user.getMdfyDate())
                .inpUserId(user.getInpUserId())
                .mdfyUserId(user.getMdfyUserId())
                .build();
    }
}
