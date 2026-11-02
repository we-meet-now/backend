package com.wemeetnow.chat_service.config;

import java.security.Principal;

public record ChatUserPrincipal(
        Long userId, String uuid, boolean guest, String username, String guestToken, String isRefreshed)
        implements Principal {

    public ChatUserPrincipal(Long userId, String uuid, boolean guest, String username) {
        this(userId, uuid, guest, username, null, "N");
    }

    @Override
    public String getName() {
        return userId.toString();
    }
}
