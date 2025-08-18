package com.wemeetnow.auth_service.domain.enums;

import lombok.Getter;

@Getter
public enum FriendStatus {
    BLOCK("BLOCK", "차단"), HIDE("HIDE", "숨김"), FAVORITE("FAVORITE", "즐겨찾기"), NEW("NEW", "신규"), REPORTED("REPORTED", "신고"), WAIT("WAIT", "수락대기"), DEL("DEL", "삭제");
    String status;
    String desc;
    FriendStatus(String status, String desc){
        this.status = status;
        this.desc = desc;
    }

    public static FriendStatus getFriendStatusFromStr(String friendStatusStr) {
        FriendStatus retFriendStatus = null;
        for (FriendStatus fs : FriendStatus.values()) {
            if (fs.getStatus().equals(friendStatusStr)) {
                retFriendStatus = fs;
                break;
            }
        }
        return retFriendStatus;
    }
}
