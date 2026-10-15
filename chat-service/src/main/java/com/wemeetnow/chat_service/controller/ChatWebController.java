package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.AuthUserResponse;
import com.wemeetnow.chat_service.dto.CommonApiResponse;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import com.wemeetnow.chat_service.domain.Chat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat/v1/chats")
@RequiredArgsConstructor
public class ChatWebController {
    private final ChatService chatService;
    private final ChatRoomService chatRoomService;
    private final String AUTH_HEADER = "Authorization";

    @Operation(
            summary = "전체 채팅 테이블 조회",
            description = "로그인 없이 모든 채팅 행과 메시지 내용을 조회합니다. 이 API는 채팅 메시지 및 사용자 식별 정보를 공개합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅 테이블 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/all")
    @ResponseBody
    public ResponseEntity<CommonApiResponse<List<Chat>>> getAllChats() {
        return ResponseEntity.ok(CommonApiResponse.<List<Chat>>builder()
                .statusCode("2000")
                .data(chatService.getAllChats())
                .message("전체 채팅 테이블 조회 성공")
                .build());
    }


    @PostMapping("/check-access-token")
    public void checkAccessToken(@RequestHeader(AUTH_HEADER) String token) {
        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            log.info("authUserResponse.getUserId(): {}", authUserResponse.getUserId());
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
        }
    }
}
