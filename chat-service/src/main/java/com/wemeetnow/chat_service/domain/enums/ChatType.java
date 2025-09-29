package com.wemeetnow.chat_service.domain.enums;

import lombok.Getter;

@Getter
public enum ChatType {
    ENTER("ENTER", "참여"), LEAVE("LEAVE", "떠나다"), CHAT("CHAT", "일반메시지");
    String type;
    String desc;

    ChatType(String type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public static ChatType getChatTypeFromStr(String chatTypeStr) {
        ChatType chatType = null;
        for (ChatType ct : chatType.values()) {
            if(ct.getType().equals(chatTypeStr)) {
                chatType = ct;
                break;
            }
        }
        return chatType;
    }
}
