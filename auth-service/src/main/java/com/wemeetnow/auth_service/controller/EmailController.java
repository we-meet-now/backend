package com.wemeetnow.auth_service.controller;

import com.wemeetnow.auth_service.dto.CommonApiResponse;
import com.wemeetnow.auth_service.dto.VerifyEmailRequestDto;
import com.wemeetnow.auth_service.dto.VerificationRequestDto;
import com.wemeetnow.auth_service.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이메일 인증 API", description = "회원가입 전 이메일 인증 코드 발송 및 검증 API")
@Slf4j
@RestController
@RequestMapping("/api/auth/v1/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @Operation(
            summary = "인증코드 이메일 발송",
            description = "이메일을 입력하여 인증 코드를 발송합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 이메일 발송 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (이메일 API 오류 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/send")
    public ResponseEntity<CommonApiResponse<Void>> sendEmail(@RequestBody VerifyEmailRequestDto requestDto) {
        emailService.sendVerificationEmail(requestDto.getEmail());

        return ResponseEntity.ok(CommonApiResponse.<Void>builder()
                .statusCode("2000")
                .data(null)
                .message("인증 이메일이 발송되었습니다. (5분 동안 유효)")
                .build());
    }

    @Operation(
            summary = "이메일 인증",
            description = "이메일과 인증 코드를 입력하여 이메일을 인증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일 인증 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/verify")
    public ResponseEntity<CommonApiResponse<Void>> verifyEmail(@RequestBody VerificationRequestDto requestDto) {
        boolean isVerified = emailService.verifyEmail(requestDto.getEmail(), requestDto.getCode());
        log.info("Email verification attempt for {}, {}: {}", requestDto.getEmail(), requestDto.getCode(), isVerified ? "SUCCESS" : "FAILURE");

        if (isVerified) {
            return ResponseEntity.ok(CommonApiResponse.<Void>builder()
                    .statusCode("2000")
                    .data(null)
                    .message("이메일 인증이 완료되었습니다.")
                    .build());
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    CommonApiResponse.<Void>builder()
                            .statusCode("4000")
                            .data(null)
                            .message("이메일 인증에 실패했습니다. 인증코드를 다시 확인해주세요.")
                            .build()
            );
        }
    }
}