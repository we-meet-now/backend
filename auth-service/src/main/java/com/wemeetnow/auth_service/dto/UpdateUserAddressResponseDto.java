package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAddressResponseDto {
    private Long userId;
    private String postCd1;
    private String addr1;
    private String detailAddr1;
    private String message;

    public static UpdateUserAddressResponseDto fromEntity(User user, String message) {
        return UpdateUserAddressResponseDto.builder()
                .userId(user.getId())
                .postCd1(user.getPostCd1())
                .addr1(user.getAddr1())
                .detailAddr1(user.getDetailAddr1())
                .message(message)
                .build();
    }
}
