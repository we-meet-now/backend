package com.wemeetnow.auth_service.domain;

import com.wemeetnow.auth_service.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Table(name = "user")
public class User extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String nickname;
    private String provider;
    private Boolean emailAuth;
    private String imgUrl = "https://velog.velcdn.com/images/kyunghwan1207/post/ce34e29d-643a-4d52-8c1f-6f55232294c7/image.png";

    @Enumerated(EnumType.STRING)
    private Role role;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true) // 양방향 매핑하기 위함
    private List<Friend> friends = new ArrayList<>();

    // TODO 채팅 필드 필요

    @Nullable
    private String phoneNumber;

    @Builder
    public User(String username, String email, String password, String nickname, String provider, Boolean emailAuth, Role role) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.emailAuth = emailAuth;
        this.role = role;
    }

    public void emailVerifiedSuccess(){
        this.emailAuth = true;
    }

    public User toDto() {
        return User.builder()
                .email(this.email)
                .password(this.password)
                .username(this.username)
                .role(this.role)
                .build();
    }
}
