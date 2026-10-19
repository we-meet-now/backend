package com.wemeetnow.chat_service.config;

import java.security.Principal;

public record ChatUserPrincipal(Long userId, String uuid, boolean guest) implements Principal {

    @Override
    public String getName() {
        return userId.toString();
    }
}
