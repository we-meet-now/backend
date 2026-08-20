package com.wemeetnow.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserAddressRequestDto {
    private String postCd1;
    private String addr1;
    private String detailAddr1;
}
