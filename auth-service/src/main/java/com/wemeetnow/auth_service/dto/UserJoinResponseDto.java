package com.wemeetnow.auth_service.dto;

import com.wemeetnow.auth_service.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserJoinResponseDto {
    private Long userId;
    public static UserJoinResponseDto fromEntity(User user){
        return new UserJoinResponseDto(user.getId());
    }
}
