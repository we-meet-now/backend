package com.wemeetnow.auth_service.domain.enums;

import lombok.Getter;

@Getter
public enum FriendStatus {
    BLOCK("BLOCK", "차단"), HIDE("HIDE", "숨김"), FAVORITE("FAVORITE", "즐겨찾기"), NEW("NEW", "신규"), REPORTED("REPORTED", "신고");
    String status;
    String desc;
    FriendStatus(String status, String desc){
        this.status = status;
        this.desc = desc;
    }
}
