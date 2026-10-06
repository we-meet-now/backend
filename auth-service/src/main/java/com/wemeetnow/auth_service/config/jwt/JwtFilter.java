package com.wemeetnow.auth_service.config.jwt;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    private final UserService userService;

    // SecurityConfig의 permitAll 경로 중, 게스트(비로그인) 토큰으로 호출되는 엔드포인트는
    // JwtFilter에서 사용자 조회(getUserByEmail)를 시도하지 않도록 필터 자체를 건너뛴다.
    private static final List<RequestMatcher> NO_LOGIN_MATCHERS = List.of(
            new AntPathRequestMatcher("/api/auth/v1/users/no-login/**"),
            new AntPathRequestMatcher("/api/auth/v1/users/is-guest-token")
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return NO_LOGIN_MATCHERS.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // token은 변경되면 안되기에 final키워드 사용
        final String authorizationHeader = request.getHeader(AUTHORIZATION);
        log.info("authorizatonHeader: {}", authorizationHeader);

        if(authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        // token 분리
        String token;
        try {
            token = authorizationHeader.split(" ")[1];
        } catch (Exception e){
            log.error("toekn 추출에 실패했습니다");
            filterChain.doFilter(request, response);
            return;
        }
        // token 유효한지 check
        if(JwtUtil.isExpired(token)){
            log.info("token이 유효하지 않습니다");
            filterChain.doFilter(request, response);
            return;
        }
        String email = JwtUtil.getEmail(token);
        log.info("try access user's email = " + email);
        // 게스트(비로그인) 토큰은 email claim이 없으므로 사용자 조회를 시도하지 않고 그대로 통과시킨다.
        // (그대로 조회하면 UsernameNotFoundException -> 403으로 이어짐)
        if (email == null || email.isBlank() || "emptyString".equals(email)) {
            log.info("게스트 토큰으로 판단되어 인증 없이 통과시킵니다.");
            filterChain.doFilter(request, response);
            return;
        }

        User findUser;
        try {
            findUser = userService.getUserByEmail(email);
        } catch (Exception e) {
            log.error("사용자 조회 실패로 인증 없이 통과시킵니다: {}", e.getMessage());
            filterChain.doFilter(request, response);
            return;
        }
        log.info("findUser's role = " + findUser.getRole());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken("", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))); // 문열어주기
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request)); // 다음인증에 필요한 정보를 넘김
        SecurityContextHolder.getContext().setAuthentication(authenticationToken); // 권한 부여
        filterChain.doFilter(request, response);
    }
}
