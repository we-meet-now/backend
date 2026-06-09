package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.service.ChatRoomService;
import com.wemeetnow.chat_service.service.ChatService;
import com.wemeetnow.chat_service.service.ChatUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "채팅방 API", description = "채팅방 생성, 조회, 입장, 퇴장 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final String AUTH_HEADER = "Authorization";

    @Value("${chat-service.url}")
    private String chatServiceUrl;

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "JWT 토큰으로 로그인한 사용자가 참여 중인 채팅방 목록과 " +
                          "각 채팅방의 읽지 않은 메시지 수를 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatRoomListResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<CommonApiResponse<ChatRoomListResponseDto>> getChatRoomListByUserId(
            @RequestHeader(AUTH_HEADER) String token,
            HttpServletRequest request) {
        log.info("request: {}", request);
        AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
        Long loginedUserId = authUserResponse.getUserId();
        List<ChatRoom> chatRoomList = chatRoomService.findByUserId(loginedUserId);

        List<ChatRoomWithNotReadCountDto> chatRoomListWithNotReadCount = chatRoomList.stream()
                .map(chatRoom -> new ChatRoomWithNotReadCountDto(
                        chatRoom,
                        chatService.getNotReadCountByRoomIdAndUserId(chatRoom.getChatRoomId(), loginedUserId)
                ))
                .toList();

        ChatRoomListResponseDto responseDto = ChatRoomListResponseDto.builder()
                .loginedUserId(loginedUserId)
                .chatRoomList(chatRoomListWithNotReadCount)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<ChatRoomListResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("채팅방 목록 조회 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 입장 (채팅 내역 조회)",
            description = "채팅방 ID로 해당 채팅방의 전체 채팅 내역을 조회합니다. " +
                          "로그인한 사용자 ID와 채팅 목록을 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 입장 및 채팅 내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = EnterChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, 잘못된 roomId 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/roomId={roomId}")
    public ResponseEntity<CommonApiResponse<EnterChatRoomResponseDto>> enterChatRoom(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request,
            @RequestHeader(AUTH_HEADER) String token) {
        log.info("roomId: {}", roomId);
        AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
        Long loginedUserId = authUserResponse.getUserId();
        List<Chat> chatList = chatService.getChatList(roomId, loginedUserId);

        EnterChatRoomResponseDto responseDto = EnterChatRoomResponseDto.builder()
                .loginedUserId(loginedUserId)
                .chatList(chatList)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<EnterChatRoomResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("채팅방 입장 성공")
                .build());
    }


    /**
     * 사용자 여러명 초대하면서 생성하는 채팅방
     */
    @Operation(
            summary = "채팅방 생성 (다중 참여자 초대)",
            description = "채팅방 이름과 참여자 ID 목록을 받아 채팅방을 생성하고, " +
                          "요청한 사용자 및 초대된 참여자들을 채팅방에 등록합니다. " +
                          "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateOneChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, DB 저장 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/create-one")
    public ResponseEntity<CommonApiResponse<CreateOneChatRoomResponseDto>> createOnChatRoom(
            @RequestBody CreateChatRoomRequestDto createChatRoomRequestDto,
            HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String accessToken = authorizationHeader.replace("Bearer ", "");
        AuthUserDto authUserDto = chatRoomService.fetchUserFromAuthService(accessToken);
        String inpUserId = String.valueOf(authUserDto.getUserId());
        Long chatRoomId = chatRoomService.createOnChatRoom(
                inpUserId,
                createChatRoomRequestDto.getChatRoomNm(),
                createChatRoomRequestDto.getParticipantIds()
        );
        CreateOneChatRoomResponseDto responseDto = new CreateOneChatRoomResponseDto(chatRoomId);
        CommonApiResponse<CreateOneChatRoomResponseDto> response = CommonApiResponse.<CreateOneChatRoomResponseDto>builder()
                .statusCode("2001")
                .data(responseDto)
                .message("채팅방 생성 성공")
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "채팅방 입장 처리 및 읽음 처리",
            description = "채팅방에 입장할 때 읽지 않은 메시지를 읽음 처리합니다. " +
                          "읽음 처리된 메시지 수를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "읽음 처리 성공",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class)))
    })
    @GetMapping("/enter-room/roomId={roomId}")
    public ResponseEntity<EnterRoomResponseDto> enterRoomAndMarkRead(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request) {
        String statusCode = "5000";
        HttpStatus httpStatus = HttpStatus.OK;
        EnterRoomResponseDto responseDto = null;
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = authorizationHeader.replace("Bearer ", "");
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            Long loginedUserId = authUserResponse.getUserId();
            responseDto = chatRoomService.enterRoomAndMarkRead(roomId, loginedUserId);
        } catch (Exception e) {
            log.error("raised error: {}", e.getMessage());
            statusCode = "5005";
            responseDto = EnterRoomResponseDto.builder()
                    .statusCode(statusCode)
                    .statusMsg("fail")
                    .markedReadCount(0)
                    .build();
        }
        return ResponseEntity.status(httpStatus).body(responseDto);
    }

    @Operation(
            summary = "채팅방 퇴장",
            description = "로그인한 사용자를 해당 채팅방에서 퇴장(참여자 제거)시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 퇴장 성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/leave-room/roomId={roomId}")
    public ResponseEntity<CommonApiResponse<Void>> leaveChatRoom(
            @PathVariable("roomId") Long roomId,
            @RequestHeader("Authorization") String token) {
        AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
        Long userId = authUserResponse.getUserId();
        chatRoomService.leaveRoom(roomId, userId);

        return ResponseEntity.ok(CommonApiResponse.<Void>builder()
                .statusCode("2000")
                .data(null)
                .message("채팅방 퇴장 성공")
                .build());
    }

    @Operation(
            summary = "익명(비로그인) 채팅방 생성",
            description = "로그인한 사용자가 모임 유형(meetType)을 지정하여 익명 채팅방을 생성합니다. " +
                          "생성 완료 후 익명 참여자 초대 URL을 반환합니다. " +
                          "meetType이 비어있으면 '아무거나 다 좋은 모임'으로 채팅방 이름이 설정됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "익명 채팅방 생성 성공",
                    content = @Content(schema = @Schema(implementation = InviteAnonymousResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, DB 저장 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/create-anonymous-room")
    public ResponseEntity<CommonApiResponse<InviteAnonymousResponseDto>> inviteAnonymous(
            HttpServletRequest request,
            @RequestBody CreateAnonymousChatRoomRequestDto requestDto) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = authorizationHeader.replace("Bearer ", "");
        AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
        Long loginedUserId = authUserResponse.getUserId();
        log.info("loginedUserId: {}", loginedUserId);

        ChatUserInfo loginedUserInfo = chatRoomService.fetchUserInfoFromAuthService(token);
        String chatRoomNm = requestDto.getMeetType().isBlank()
                ? loginedUserInfo.getUsername() + "의 아무거나 다 좋은 모임"
                : loginedUserInfo.getUsername() + "의 " + requestDto.getMeetType();

        Long chatRoomId = chatRoomService.createAnonymousChatRoom(loginedUserId, chatRoomNm, requestDto);
        String inviteUrl = chatServiceUrl + "/chat-participants/anonymous-chat-roomId=" + chatRoomId;

        InviteAnonymousResponseDto responseDto = InviteAnonymousResponseDto.builder()
                .chatRoomId(chatRoomId)
                .inviteAnonymousUrl(inviteUrl)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<InviteAnonymousResponseDto>builder()
                .statusCode("2000")
                .data(responseDto)
                .message("익명 채팅방 생성 성공")
                .build());
    }
}
