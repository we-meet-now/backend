package com.wemeetnow.auth_service.service;

import com.wemeetnow.auth_service.domain.UserLog;
import com.wemeetnow.auth_service.domain.enums.ErrorCode;
import com.wemeetnow.auth_service.exception.CustomException;
import com.wemeetnow.auth_service.exception.InvalidPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserLogService {

    private final UserLogPersistenceService persistenceService;

    /**
     * Executes an API action and inserts its access log whether it returns or throws.
     * The status resolver should return the HTTP status represented by the action's result.
     */
    public <T> T executeWithLogging(
            HttpServletRequest request,
            Supplier<String> inpUserIdResolver,
            String menuLink,
            Supplier<T> action,
            ToIntFunction<T> responseStatusResolver
    ) {
        long startedAtNanos = System.nanoTime();
        int responseStatus = ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value();
        try {
            T result = action.get();
            responseStatus = responseStatusResolver.applyAsInt(result);
            return result;
        } catch (RuntimeException exception) {
            responseStatus = resolveFailureStatus(exception);
            throw exception;
        } finally {
            long durationMs = (System.nanoTime() - startedAtNanos) / 1_000_000;
            recordUserLog(request, resolveInpUserId(inpUserIdResolver), menuLink, responseStatus, durationMs);
        }
    }

    /** Common entry point for code that has already executed an API action. */
    public void recordUserLog(
            HttpServletRequest request,
            String inpUserId,
            String menuLink,
            int responseStatus,
            long durationMs
    ) {
        String normalizedMenuLink = fit(menuLink, 500, "unknown");
        String requestUri = fit(request.getRequestURI(), 2000, normalizedMenuLink);
        String userAgent = fit(request.getHeader("User-Agent"), 2000, null);

        UserLog userLog = new UserLog(
                LocalDateTime.now(),
                fit(inpUserId, 255, null),
                fit(request.getMethod(), 20, "UNKNOWN"),
                normalizedMenuLink,
                resolveUserIp(request),
                responseStatus,
                resolveRequestId(request),
                userAgent,
                Math.max(durationMs, 0L),
                requestUri,
                resolveDeviceType(userAgent)
        );
        persistenceService.insert(userLog);
    }

    private String resolveInpUserId(Supplier<String> inpUserIdResolver) {
        if (inpUserIdResolver == null) {
            return null;
        }
        try {
            return inpUserIdResolver.get();
        } catch (RuntimeException exception) {
            log.warn("Could not resolve the persisted user ID for USER_LOG", exception);
            return null;
        }
    }

    private int resolveFailureStatus(RuntimeException exception) {
        if (exception instanceof CustomException customException) {
            return customException.getErrorCode().getHttpStatus().value();
        }
        if (exception instanceof InvalidPasswordException) {
            return ErrorCode.BAD_REQUEST.getHttpStatus().value();
        }
        // GlobalExceptionHandler currently maps IllegalArgumentException to HTTP 500.
        return ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus().value();
    }

    private String resolveUserIp(HttpServletRequest request) {
        // X-Forwarded-For is expected to be sanitized by the trusted reverse proxy.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String clientIp = forwardedFor.split(",", 2)[0].trim();
            if (!clientIp.isEmpty() && clientIp.length() <= 45) {
                return clientIp;
            }
        }

        return fit(request.getRemoteAddr(), 45, "unknown");
    }

    private String resolveRequestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        if (requestId == null || requestId.isBlank()) {
            requestId = request.getHeader("X-Correlation-ID");
        }
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return fit(requestId, 100, UUID.randomUUID().toString());
    }

    private String resolveDeviceType(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "UNKNOWN";
        }

        String normalized = userAgent.toLowerCase(Locale.ROOT);
        if (normalized.matches(".*(ipad|tablet|kindle|silk|playbook).*")) {
            return "TABLET";
        }
        if (normalized.contains("android")) {
            return normalized.contains("mobile") ? "MOBILE" : "TABLET";
        }
        if (normalized.matches(".*(mobi|iphone|ipod).*")) {
            return "MOBILE";
        }
        return "PC";
    }

    private String fit(String value, int maxLength, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
