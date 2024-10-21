package com.wemeetnow.auth_service.domain.enums;

import lombok.Getter;

@Getter
public enum Role {
    ROLE_USER("ROLE_USER", "일반"), ROLE_ADMIN("ROLE_ADMIN", "관리자");
    String auth;
    String desc;
    Role(String auth, String desc){
        this.auth = auth;
        this.desc = desc;
    }
}
