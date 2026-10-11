package com.wemeetnow.auth_service.domain.enums;

import lombok.Getter;

import java.util.Locale;

@Getter
public enum FriendStatus {
    BLOCK("BLOCK", "차단"), HIDE("HIDE", "숨김"), FAVORITE("FAVORITE", "즐겨찾기"), NEW("NEW", "신규"), REPORTED("REPORTED", "신고"), WAIT("WAIT", "수락대기"), DEL("DEL", "삭제");
    private final String status;
    private final String desc;

    FriendStatus(String status, String desc) {
        this.status = status;
        this.desc = desc;
    }

    public static FriendStatus getFriendStatusFromStr(String friendStatusStr) {
        if (friendStatusStr == null || friendStatusStr.isBlank()) {
            return null;
        }
        String normalizedStatus = friendStatusStr.trim().toUpperCase(Locale.ROOT);
        for (FriendStatus friendStatus : FriendStatus.values()) {
            if (friendStatus.status.equals(normalizedStatus)) {
                return friendStatus;
            }
        }
        return null;
    }
}
