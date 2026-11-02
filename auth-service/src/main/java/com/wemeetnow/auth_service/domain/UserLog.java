package com.wemeetnow.auth_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자의 메뉴 접근 이력을 저장하는 로그 엔티티입니다.
 * 로그 데이터는 생성 후 변경하지 않도록 매핑합니다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "\"USER_LOG\"")
public class UserLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_log_id")
    private Long id;

    @Column(name = "inp_date", nullable = false, updatable = false)
    private LocalDateTime inpDate;

    @Column(name = "inp_user_id", nullable = true, length = 255, updatable = false)
    private String inpUserId;

    @Column(name = "req_method", nullable = false, length = 20, updatable = false)
    private String reqMethod;

    @Column(name = "menu_link", nullable = false, length = 500, updatable = false)
    private String menuLink;

    @Column(name = "user_ip", nullable = false, length = 45, updatable = false)
    private String userIp;

    @Column(name = "response_status", updatable = false)
    private Integer responseStatus;

    @Column(name = "request_id", length = 100, updatable = false)
    private String requestId;

    @Column(name = "user_agent", length = 2000, updatable = false)
    private String userAgent;

    @Column(name = "duration_ms", updatable = false)
    private Long durationMs;

    @Column(name = "request_uri", length = 2000, updatable = false)
    private String requestUri;

    @Column(name = "device_type", length = 20, updatable = false)
    private String deviceType;

    public UserLog(
            LocalDateTime inpDate,
            String inpUserId,
            String reqMethod,
            String menuLink,
            String userIp,
            Integer responseStatus,
            String requestId,
            String userAgent,
            Long durationMs,
            String requestUri,
            String deviceType
    ) {
        this.inpDate = inpDate;
        this.inpUserId = inpUserId;
        this.reqMethod = reqMethod;
        this.menuLink = menuLink;
        this.userIp = userIp;
        this.responseStatus = responseStatus;
        this.requestId = requestId;
        this.userAgent = userAgent;
        this.durationMs = durationMs;
        this.requestUri = requestUri;
        this.deviceType = deviceType;
    }
}
