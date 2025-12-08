package com.wemeetnow.auth_service.config.auth;

import com.wemeetnow.auth_service.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Getter
@Component
@NoArgsConstructor
public class PrincipalDetails implements UserDetails {

    private User user;
    private Map<String, Object> attributes;

    // 일반 로그인시 사용
    @Builder
    public PrincipalDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
                if (user == null) {
                    return "#Null";
                }
                return user.getRole().toString();
            }
        });
        return collect;
    }

    @Override
    public String getPassword() {
        if (this.user == null) {
            return "#NUll";
        }
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        if (this.user == null) {
            return "#Null";
        }
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}
