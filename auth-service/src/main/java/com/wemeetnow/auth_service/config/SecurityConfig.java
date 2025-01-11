package com.wemeetnow.auth_service.config;

import com.wemeetnow.auth_service.config.jwt.JwtFilter;
import com.wemeetnow.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private static final String[] PERMIT_URL = {
            "/api/v1/users/join", "/api/v1/users/login", "/api/v1/users/logout",
            "/api/v1/users/reissue", "/api/v1/users/check-is-logined"
    };
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequest ->
                        authorizeRequest
//                                .anyRequest().permitAll()
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/join")).permitAll()
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/login")).permitAll()
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/logout")).permitAll()
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/reissue")).permitAll()
                                // .requestMatchers(AntPathRequestMatcher.antMatcher(PERMIT_URL[4])).permitAll()
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/login/page")).permitAll() // 카카오로그인 테스트용
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/stores/recommend-one")).permitAll() // 맛집 추천받기 테스트용
                                .requestMatchers(AntPathRequestMatcher.antMatcher("/callback")).permitAll() // 카카오로그인 테스트용
                        )
                .headers(
                        headersConfigurer -> headersConfigurer.frameOptions(
                                HeadersConfigurer.FrameOptionsConfig::sameOrigin
                        )
                )
                .addFilterBefore(new JwtFilter(userService), UsernamePasswordAuthenticationFilter.class)
        ;
        return httpSecurity.build();
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        // 정적 리소스 spring security 대상에서 제외
        return (web) ->
                web
                        .ignoring()
                        .requestMatchers(
                                PathRequest.toStaticResources().atCommonLocations()
                        );
    }
}
