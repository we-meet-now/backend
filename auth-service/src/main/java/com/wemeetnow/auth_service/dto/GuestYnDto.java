package com.wemeetnow.auth_service.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class GuestYnDto {
    private String isGuest;
}
