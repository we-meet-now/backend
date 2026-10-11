package com.wemeetnow.auth_service.config.jwt;

import com.wemeetnow.auth_service.domain.User;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.domain.enums.Role;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.ACCESS_TOKEN_EXPIRATION_TIME;
import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.REFRESH_TOKEN_EXPIRATION_TIME;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final UserRepository userRepository;
    private static String SECRET_KEY;

    @Value("${jwt.secret}")
    private void setSecretKey(String key){
        this.SECRET_KEY = key;
    }

    public static Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey(SECRET_KEY))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException expiredError) {
            // 서명은 유효하지만 만료된 토큰인 경우, claim 정보(uuId 등)는 그대로 사용할 수 있어야 하므로
            // 예외에 담긴 claims를 반환한다. 만료 여부 자체는 isExpired()에서 별도로 판단한다.
            return expiredError.getClaims();
        } catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return null;
        }
    }

    public static String getEmail(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null) {
            return claims.get("email", String.class);
        } else {
            return "emptyString";
        }

    }
    public static Long getId(String token){
        Claims claims = extractAllClaims(token);
        if (claims != null) {
            Object userIdObj = claims.get("userId");
            if (userIdObj instanceof Integer) {
                return ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                return (Long) userIdObj;
            } else if (userIdObj instanceof String) {
                return Long.parseLong((String) userIdObj);
            }
        }
        return 0L;
    }

    /**
     * Authorization 헤더의 Bearer JWT를 검증하고, 토큰의 사용자 ID에 해당하는 DB 사용자를 반환합니다.
     * 토큰이 없거나 유효하지 않거나 사용자 레코드가 없으면 UNAUTHORIZED 예외를 발생시킵니다.
     */
    public User getAuthenticatedUser(HttpServletRequest request) {
        if (request == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty() || isExpired(token)) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        Long userId;
        try {
            userId = getId(token);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }

        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));
    }

    private static Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static Boolean isExpired(String token) {
        try {
            Claims claims = extractAllClaims(token);
            if (claims == null) {
                // extractAllClaims()는 만료/위변조 토큰에 대해 내부적으로 예외를 삼키고 null을 반환하므로
                // 이 경우도 만료(유효하지 않음)로 간주한다.
                return true;
            }
            Date expiration = claims.getExpiration();
            return expiration == null || expiration.before(new Date());
        } catch (MalformedJwtException malError) {
            log.error("Invalid JWT token(MalformedJwtException): {}", malError.getMessage());
            return true;
        } catch (ExpiredJwtException expError) {
            log.error("Expired JWT token(ExpiredJwtException): {}", expError.getMessage());
            return true;
        }
    }

    public String generateAccessToken(Long userId, String email, Role role) {
        try {
            return doGenerateToken(userId, email, role, ACCESS_TOKEN_EXPIRATION_TIME.getValue());
        } catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return "emptyString";
        }

    }

    public String generateRefreshToken(Long userId, String email, Role role) {
        try {
            return doGenerateToken(userId, email, role, REFRESH_TOKEN_EXPIRATION_TIME.getValue());
        }
         catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return "emptyString";
        }
    }

    private String doGenerateToken(Long userId, String email, Role role, long expireTime) {
        try {
            Claims claims = Jwts.claims();
            claims.put("userId", userId);
            claims.put("email", email);
            claims.put("role", role);
            log.info("22enter doGenerateToken()" + SECRET_KEY);
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                    .signWith(getSigningKey(SECRET_KEY), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return "emptyString";
        }
    }

    private String doGenerateGuestToken(String uuId, long expireTime) {
        try {
            Claims claims = Jwts.claims();
            claims.put("uuId", uuId);
            claims.put("userId", null);
            claims.put("email", null);
            claims.put("role", Role.ROLE_USER);
            log.info("22enter doGenerateToken()" + SECRET_KEY);
            return Jwts.builder()
                    .setClaims(claims)
                    .setIssuedAt(new Date(System.currentTimeMillis()))
                    .setExpiration(new Date(System.currentTimeMillis() + expireTime))
                    .signWith(getSigningKey(SECRET_KEY), SignatureAlgorithm.HS256)
                    .compact();
        } catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return "emptyString";
        }

    }


    public static Boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token);
            return !isExpired(token);
        } catch(SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature");
            return false;
        } catch(UnsupportedJwtException e) {
            log.error("Unsupported JWT token");
            return false;
        } catch(IllegalArgumentException e) {
            log.error("JWT token is invalid");
            return false;
        }
    }

    public long getRemainMilliSeconds(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        Date now = new Date();
        return expiration.getTime() - now.getTime();
    }

    public String generateGuestToken(String uuid) {
        try {
            return doGenerateGuestToken(uuid, REFRESH_TOKEN_EXPIRATION_TIME.getValue());
        }
        catch (Exception e) {
            log.error("raised error: {}",e.getMessage());
            return null;
        }
    }

    public static boolean hasUUID(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null) {
            return claims.get("uuId") != null;
        }
        return false;
    }
    public static String getUUID(String token) {
        Claims claims = extractAllClaims(token);
        if (claims != null) {
            Object uuIdObj = claims.get("uuId");
            if (uuIdObj instanceof String) {
                return (String) uuIdObj;
            }
        }
        return null;
    }
}