package com.wemeetnow.auth_service.config;

import com.wemeetnow.auth_service.config.jwt.JwtFilter;
import com.wemeetnow.auth_service.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserService userService;

    @Value("${front.url}")
    private String frontUrl;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.addFilterBefore(corsFilter(), CorsFilter.class)
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizeRequest ->
                        authorizeRequest
//                                .anyRequest().permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/join")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/login")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/logout")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/reissue")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/get-id")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/get-user-info")).authenticated()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/login/page")).permitAll() // 카카오로그인 테스트용
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/stores/**")).permitAll() // 맛집 추천받기 테스트용
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/callback")).permitAll() // 카카오로그인 테스트용
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui/**")).permitAll() // Swagger-ui
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll() // Swagger-ui
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/favicon.ico")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher( "/error")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-resources/**")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/api-docs/**")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/friends/**")).permitAll()
                                 .requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/users/create-random-nickname")).permitAll()
                        )
                .headers(
                        headersConfigurer -> headersConfigurer.frameOptions(
                                HeadersConfigurer.FrameOptionsConfig::disable
                        )
                )
                .addFilterBefore(new JwtFilter(userService), UsernamePasswordAuthenticationFilter.class);
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

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin(frontUrl);       // application.properties의 front.url (환경별 관리)
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
