package com.wemeetnow.auth_service.config.jwt;

import com.wemeetnow.auth_service.domain.enums.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.ACCESS_TOKEN_EXPIRATION_TIME;
import static com.wemeetnow.auth_service.config.jwt.JwtExpirationEnums.REFRESH_TOKEN_EXPIRATION_TIME;

@Slf4j
@Component
public class JwtUtil {
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

    private static Key getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public static Boolean isExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
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
}