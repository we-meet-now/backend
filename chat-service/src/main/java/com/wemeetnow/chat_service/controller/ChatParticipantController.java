package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.client.AuthServiceClient;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.service.ChatParticipantService;
import com.wemeetnow.chat_service.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "채팅 참여자 API", description = "채팅방 참여자 조회 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/v1/chat-participants")
public class ChatParticipantController {
    private final ChatRoomService chatRoomService;
    private final ChatParticipantService chatParticipantService;
    private final AuthServiceClient authServiceClient;

    /**
     * 채팅방 참여한 User 정보 조회
     */
    @Operation(
            summary = "채팅방 참여자 목록 조회",
            description = "채팅방 ID로 해당 채팅방에 참여 중인 사용자 ID 목록과 " +
                          "각 사용자의 상세 정보(이름, 이메일, 프로필 등)를 반환합니다. " +
                          "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "참여자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatParticipantsResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, 잘못된 roomId 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/roomId={roomId}")
    public ResponseEntity<CommonApiResponse<ChatParticipantsResponseDto>> getChatParticipants(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request) {
        log.info("getChatParticipants roomId: {}", roomId);

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authorizationHeader.replace("Bearer ", "");
        AuthUserDto authUserDto = chatRoomService.fetchUserFromAuthService(accessToken);
        Long loginedUserId = authUserDto.getUserId();

        List<Long> userIdList = chatParticipantService.findByChatRoomId(roomId, loginedUserId);

        List<ChatParticipantUserDto> userInfoList = userIdList.stream()
                .map(authServiceClient::getUserById)
                .toList();

        ChatParticipantsResponseDto responseDto = ChatParticipantsResponseDto.builder()
                .loginedUserId(loginedUserId)
                .userIdList(userIdList)
                .userInfoList(userInfoList)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<ChatParticipantsResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("채팅방 참여자 목록 조회 성공")
                .build());
    }

    /**
     * 채팅방 비회원으로 입장한 User 정보 조회
     */
    @Operation(
            summary = "익명 채팅방 참여자 목록 조회",
            description = "익명(비회원) 채팅방 ID로 해당 채팅방의 참여자 정보와 " +
                          "각 참여자의 사용자 상세 정보를 반환합니다. " +
                          "토큰 인증 없이 접근 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "익명 채팅방 참여자 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = AnonymousChatParticipantsResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (잘못된 roomId 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/anonymous-chat-roomId={roomId}")
    public ResponseEntity<CommonApiResponse<AnonymousChatParticipantsResponseDto>> getAnonymousChatParticipants(
            @PathVariable("roomId") Long roomId) {
        log.info("getAnonymousChatParticipants roomId: {}", roomId);

        var chatParticipantList = chatParticipantService.findByAnonymousChatRoomId(roomId);

        List<ChatParticipantUserDto> userInfoList = chatParticipantList.stream()
                .map(participant -> authServiceClient.getUserById(participant.getUserId()))
                .toList();

        AnonymousChatParticipantsResponseDto responseDto = AnonymousChatParticipantsResponseDto.builder()
                .chatParticipantList(chatParticipantList)
                .userInfoList(userInfoList)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<AnonymousChatParticipantsResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("익명 채팅방 참여자 목록 조회 성공")
                .build());
    }
}
