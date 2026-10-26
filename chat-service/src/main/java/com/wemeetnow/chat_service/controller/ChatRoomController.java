package com.wemeetnow.chat_service.controller;

import com.wemeetnow.chat_service.domain.Chat;
import com.wemeetnow.chat_service.domain.ChatRoom;
import com.wemeetnow.chat_service.constant.ApiStatusCode;
import com.wemeetnow.chat_service.dto.*;
import com.wemeetnow.chat_service.exception.ChatRoomInviteException;
import com.wemeetnow.chat_service.service.ChatGuestTokenInfo;
import com.wemeetnow.chat_service.service.CommonService;
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
@RequestMapping("/api/chat/v1/chat-rooms")
public class ChatRoomController {
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final CommonService commonService;
    private final String AUTH_HEADER = "Authorization";

    @Value("${chat-service.url}")
    private String chatServiceUrl;

    @Operation(
            summary = "전체 채팅방 테이블 조회",
            description = "로그인 없이 모든 채팅방 행을 조회합니다. 입장 코드 등 채팅방의 전체 컬럼이 반환됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 테이블 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<CommonApiResponse<List<ChatRoom>>> getAllChatRooms() {
        return ResponseEntity.ok(CommonApiResponse.<List<ChatRoom>>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(chatRoomService.getAllChatRooms())
                .message("전체 채팅방 테이블 조회 성공")
                .build());
    }

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
                .statusCode(ApiStatusCode.SUCCESS.getCode())
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
                .statusCode(ApiStatusCode.SUCCESS.getCode())
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
    @PostMapping("/create-w-participants")
    public ResponseEntity<CommonApiResponse<CreateOneChatRoomResponseDto>> createChatRoomWithParticipants(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody CreateChatRoomRequestDto createChatRoomRequestDto) {
        String inpUserId = String.valueOf(chatService.isValidAccessToken(token).getUserId());
        ChatRoom chatRoom = chatRoomService.createChatRoomWithParticipants(
                inpUserId,
                createChatRoomRequestDto.getChatRoomNm(),
                createChatRoomRequestDto.getParticipantIds()
        );
        CreateOneChatRoomResponseDto responseDto = CreateOneChatRoomResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .enterCode(chatRoom.getEnterCode())
                .build();
        CommonApiResponse<CreateOneChatRoomResponseDto> response = CommonApiResponse.<CreateOneChatRoomResponseDto>builder()
                .statusCode(ApiStatusCode.CREATED.getCode())
                .data(responseDto)
                .message("채팅방 생성 성공")
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /**
     * 사용자 초대 없이 생성하는 채팅방
     */
    @Operation(
            summary = "채팅방 생성 (참여자 정보 없이 생성. 즉, 빈배열 보내도 됨)",
            description = "채팅방 이름을 받아 채팅방을 생성하고, " +
                    "Authorization 헤더의 JWT 토큰으로 요청자를 식별합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 생성 성공",
                    content = @Content(schema = @Schema(implementation = CreateOneChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, DB 저장 실패 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/create")
    public ResponseEntity<CommonApiResponse<CreateOneChatRoomResponseDto>> createChatRoom(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody CreateChatRoomRequestDto createChatRoomRequestDto) {
        String inpUserId = String.valueOf(chatService.isValidAccessToken(token).getUserId());
        ChatRoom chatRoom = chatRoomService.createChatRoom(
                inpUserId,
                createChatRoomRequestDto.getChatRoomNm()
        );
        CreateOneChatRoomResponseDto responseDto = CreateOneChatRoomResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .enterCode(chatRoom.getEnterCode())
                .build();
        CommonApiResponse<CreateOneChatRoomResponseDto> response = CommonApiResponse.<CreateOneChatRoomResponseDto>builder()
                .statusCode(ApiStatusCode.CREATED.getCode())
                .data(responseDto)
                .message("채팅방 생성 성공")
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(
            summary = "채팅방 사용자 초대",
            description = "요청한 사용자가 채팅방 참여자인지 확인하고, 유효한 신규 사용자들을 원자적으로 초대합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 초대 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 본문",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authorization 토큰이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "403", description = "요청자가 채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 초대 대상 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "409", description = "초대 대상 중 이미 채팅방 참여자가 있음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @PostMapping("/invite")
    public ResponseEntity<CommonApiResponse<InviteChatParticipantsResponseDto>> inviteChatParticipants(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody InviteChatParticipantsRequestDto requestDto) {
        AuthUserResponse authUserResponse;
        try {
            authUserResponse = chatService.isValidAccessToken(token);
        } catch (IllegalArgumentException e) {
            throw new ChatRoomInviteException(ApiStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 Access Token입니다.");
        }

        if (authUserResponse == null || authUserResponse.getUserId() == null) {
            throw new ChatRoomInviteException(ApiStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                    "Access Token에서 사용자 정보를 확인할 수 없습니다.");
        }

        List<Long> invitedUserIds = chatRoomService.inviteParticipants(
                requestDto != null ? requestDto.getRoomId() : null,
                authUserResponse.getUserId(),
                requestDto != null ? requestDto.getInvitedUserIdList() : null,
                token
        );
        InviteChatParticipantsResponseDto responseDto = InviteChatParticipantsResponseDto.builder()
                .roomId(requestDto.getRoomId())
                .invitedUserIdList(invitedUserIds)
                .invitedCount(invitedUserIds.size())
                .build();

        return ResponseEntity.ok(CommonApiResponse.<InviteChatParticipantsResponseDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(responseDto)
                .message("채팅방 사용자 초대 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 사용자 퇴장시키기(방생성자가 다수참여자 퇴장 가능)",
            description = "채팅방 생성자만 지정한 사용자를 채팅방에서 퇴장시킬 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용자 퇴장 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 요청 본문"),
            @ApiResponse(responseCode = "401", description = "Authorization 토큰이 유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "요청자가 채팅방 생성자가 아님"),
            @ApiResponse(responseCode = "404", description = "채팅방 또는 퇴장 대상 사용자를 찾을 수 없음")
    })
    @PostMapping("/remove-participants")
    public ResponseEntity<CommonApiResponse<RemoveChatParticipantsResponseDto>> removeChatParticipants(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String token,
            @RequestBody RemoveChatParticipantsRequestDto requestDto) {
        AuthUserResponse authUserResponse;
        try {
            authUserResponse = chatService.isValidAccessToken(token);
        } catch (IllegalArgumentException e) {
            throw new ChatRoomInviteException(ApiStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                    "유효하지 않은 Access Token입니다.");
        }

        if (authUserResponse == null || authUserResponse.getUserId() == null) {
            throw new ChatRoomInviteException(ApiStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
                    "Access Token에서 사용자 정보를 확인할 수 없습니다.");
        }

        Long roomId = requestDto != null ? requestDto.getRoomId() : null;
        List<Long> removedUserIds = chatRoomService.removeParticipants(
                roomId,
                authUserResponse.getUserId(),
                requestDto != null ? requestDto.getRemovedUserIdList() : null,
                token
        );
        RemoveChatParticipantsResponseDto responseDto = RemoveChatParticipantsResponseDto.builder()
                .roomId(roomId)
                .removedUserIdList(removedUserIds)
                .removedCount(removedUserIds.size())
                .build();

        return ResponseEntity.ok(CommonApiResponse.<RemoveChatParticipantsResponseDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(responseDto)
                .message("채팅방 사용자 퇴장 성공")
                .build());
    }

    @Operation(
            summary = "채팅방 입장 및 채팅 내역 조회",
            description = "Authorization 헤더의 사용자 또는 게스트 토큰으로 요청자를 확인합니다. " +
                    "요청 본문의 필수 roomId 채팅방 참여자만 채팅 내역을 조회할 수 있으며, " +
                    "성공하면 채팅별 작성자 userId와 username을 포함한 목록을 반환하고 읽지 않은 메시지를 읽음 처리합니다. " +
                    "해당 사용자 프로필이 없거나 username이 null이면 username도 null로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 입장 및 채팅 내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class)))
    })
    @PostMapping("/enter-room")
    public ResponseEntity<EnterRoomResponseDto> enterRoomAndMarkRead(
            @RequestBody EnterRoomRequestDto requestDto,
            HttpServletRequest request) {
        try {
            String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring("Bearer ".length());
                Long userId;
                AuthGuestResponse guestAuthResponse = null;
                GuestYnDto guestYnDto = chatRoomService.isGuestToken(token);
                if (guestYnDto != null && "Y".equals(guestYnDto.getIsGuestYn())) {
                    guestAuthResponse = chatService.isValidGuestToken(token, requestDto.getUuId());
                    userId = guestAuthResponse.getUserId();
                } else {
                    AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
                    userId = authUserResponse.getUserId();
                }

                if (!chatRoomService.isMember(requestDto.getRoomId(), userId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(EnterRoomResponseDto.builder()
                            .statusCode(ApiStatusCode.CHAT_ROOM_ACCESS_DENIED.getCode())
                            .statusMsg("채팅방 접근 권한이 없습니다.")
                            .markedReadCount(0)
                            .loginedUserId(userId)
                            .build());
                }

                EnterRoomResponseDto responseDto =
                        chatRoomService.enterRoomAndMarkRead(requestDto.getRoomId(), userId);
                responseDto.setLoginedUserId(userId);
                if (ApiStatusCode.SUCCESS.getCode().equals(responseDto.getStatusCode())) {
                    List<Chat> chats = chatService.getChatList(requestDto.getRoomId(), userId);
                    responseDto.setChatList(guestAuthResponse == null
                            ? chatService.getChatResponseList(chats, authorizationHeader)
                            : chats.stream().map(ChatResponseDto::fromEntity).toList());
                }
                if (guestAuthResponse != null) {
                    responseDto.setUuId(guestAuthResponse.getUuId());
                    responseDto.setGuestToken(guestAuthResponse.getGuestToken());
                    responseDto.setIsRefreshed(guestAuthResponse.getIsRefreshed());
                }
                return ResponseEntity.status(ApiStatusCode.SUCCESS.getCode().equals(responseDto.getStatusCode())
                                ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(responseDto);
            } else {
                ChatGuestTokenInfo guestTokenInfo = chatRoomService.generateGuestToken();
                if (guestTokenInfo != null) {
                    return ResponseEntity.ok(EnterRoomResponseDto.builder()
                            .statusCode(ApiStatusCode.GUEST_TOKEN_ISSUED.getCode())
                            .statusMsg("success")
                            .markedReadCount(0)
                            .guestToken(guestTokenInfo.getGuestToken())
                            .uuId(guestTokenInfo.getUuId())
                            .isRefreshed("N")
                            .build());
                }
                throw new IllegalStateException("게스트 토큰을 발급하지 못했습니다.");
            }
        } catch (Exception e) {
            log.error("채팅방 입장 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EnterRoomResponseDto.builder()
                    .statusCode(ApiStatusCode.INTERNAL_SERVER_ERROR.getCode())
                    .statusMsg("fail")
                    .markedReadCount(0)
                    .build());
        }
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
                .statusCode(ApiStatusCode.SUCCESS.getCode())
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

        ChatUserInfo loginedUserInfo = commonService.fetchUserInfoFromAuthService(token);
        String chatRoomNm = requestDto.getMeetType().isBlank()
                ? loginedUserInfo.getUsername() + "의 아무거나 다 좋은 모임"
                : loginedUserInfo.getUsername() + "의 " + requestDto.getMeetType();

        ChatRoom chatRoom = chatRoomService.createAnonymousChatRoom(loginedUserId, chatRoomNm, requestDto);
        String inviteUrl = chatServiceUrl + "/chat-participants/anonymous-chat-roomId=" + chatRoom.getChatRoomId();

        InviteAnonymousResponseDto responseDto = InviteAnonymousResponseDto.builder()
                .chatRoomId(chatRoom.getChatRoomId())
                .inviteAnonymousUrl(inviteUrl)
                .build();

        return ResponseEntity.ok(CommonApiResponse.<InviteAnonymousResponseDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(responseDto)
                .message("익명 채팅방 생성 성공")
                .build());
    }
}
