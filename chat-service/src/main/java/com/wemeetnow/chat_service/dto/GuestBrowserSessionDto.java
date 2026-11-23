package com.wemeetnow.chat_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GuestBrowserSessionDto {
    private Long userId;
    private String uuid;
    private String username;
}
