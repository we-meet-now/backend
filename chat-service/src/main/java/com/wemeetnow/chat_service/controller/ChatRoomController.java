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
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Tag(name = "채팅방 API", description = "채팅방 생성, 조회, 입장, 퇴장 관련 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/v1/chat-rooms")
public class ChatRoomController {
    private static final String GUEST_SESSION_COOKIE = "guest_session";
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

    ResponseEntity<CommonApiResponse<ChatRoomListResponseDto>> getChatRoomListByUserId(
            String token,
            String uuid
    ) {
        return getChatRoomListByUserId(token, uuid, null);
    }

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "로그인 사용자와 비로그인 게스트가 같은 API로 자신의 채팅방 목록을 조회합니다. " +
                    "로그인 사용자는 Auth 서비스의 POST /api/auth/v1/users/login 응답 data.accessToken을 " +
                    "Authorization에 Bearer 형식으로 입력하고 uuid는 비워 둡니다. " +
                    "브라우저 게스트는 Auth 서비스의 POST /api/auth/v1/users/no-login/guest-browser-sessions를 " +
                    "credentials를 포함해 호출하여 발급된 HttpOnly guest_session 쿠키를 전달합니다. 서버가 쿠키로 " +
                    "UUID와 사용자를 확인하므로 브라우저 요청에는 Authorization과 UUID가 필요하지 않습니다. " +
                    "신규 게스트는 Auth 서비스의 POST /api/auth/v1/users/no-login/guest-sessions 응답에서 " +
                    "data.guestToken을 받아 Authorization에 전달합니다. 서버가 토큰에서 UUID를 확인하므로 " +
                    "해당 guestToken을 Bearer 형식으로 입력합니다. 두 값은 같은 발급 응답의 쌍이어야 합니다. " +
                    "발급 응답의 refreshToken은 안전한 저장소에 보관하고 guestToken 만료 시 Auth 서비스의 POST " +
                    "/api/auth/v1/users/no-login/guest-sessions/refresh Request Body로 전달해 갱신하세요. " +
                    "기존 게스트는 guestToken이 유효할 때 POST /api/auth/v1/users/no-login/guest-sessions/upgrade에 " +
                    "기존 guestToken과 UUID를 함께 보내 refreshToken을 등록할 수 있습니다. " +
                    "UUID만 있고 guestToken 또는 refreshToken이 없다면 기존 게스트를 복구할 수 없습니다. " +
                    "Swagger UI에서는 Try it out을 누르고 해당 사용자 유형의 예시를 선택한 뒤 실제 발급 값으로 " +
                    "교체하여 Execute를 누르세요. UUID만 전달하거나 토큰과 UUID를 서로 다르게 조합하면 401을 반환합니다. " +
                    "신규 클라이언트는 AccessToken/guestToken 또는 guest_session 쿠키만 보내며 UUID는 URL과 요청 본문에 넣지 않습니다. " +
                    "서버가 인증 자격증명에서 UUID를 확인합니다. 성공 시 참여 채팅방과 채팅방별 미읽음 메시지 수를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class), examples = {
                            @ExampleObject(name = "로그인 사용자 성공", summary = "AccessToken으로 목록 조회",
                                    value = "{\"statusCode\":\"2000\",\"data\":{\"loginedUserId\":123,\"chatRoomList\":[{\"chatRoom\":{\"chatRoomId\":501,\"chatRoomNm\":\"A 테스트 모임\"},\"isNotReadCount\":2}]},\"message\":\"채팅방 목록 조회 성공\"}"),
                            @ExampleObject(name = "비로그인 게스트 성공", summary = "guestToken으로 목록 조회",
                                    value = "{\"statusCode\":\"2000\",\"data\":{\"loginedUserId\":456,\"chatRoomList\":[]},\"message\":\"채팅방 목록 조회 성공\"}")
                    })),
            @ApiResponse(responseCode = "401", description = "AccessToken, guestToken 또는 게스트 세션 쿠키가 없거나 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class), examples = {
                            @ExampleObject(name = "인증 값 누락", value = "{\"statusCode\":\"4010\",\"data\":null,\"message\":\"AccessToken 또는 게스트 브라우저 세션 쿠키가 필요합니다.\"}"),
                            @ExampleObject(name = "게스트 토큰 오류", value = "{\"statusCode\":\"4010\",\"data\":null,\"message\":\"유효하지 않은 게스트 토큰입니다.\"}")
                    })),
            @ApiResponse(responseCode = "500", description = "인증 서비스 오류 또는 서버 오류",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("")
    public ResponseEntity<CommonApiResponse<ChatRoomListResponseDto>> getChatRoomListByUserId(
            @Parameter(
                    description = "로그인 사용자의 AccessToken 또는 비로그인 게스트의 guestToken. " +
                            "실제 토큰을 `Bearer <token>` 형식으로 입력하세요. 게스트는 uuid와 같은 발급 응답의 토큰이어야 합니다. " +
                            "토큰이 만료되면 안전하게 보관한 refreshToken을 Auth 서비스의 POST " +
                            "/api/auth/v1/users/no-login/guest-sessions/refresh에 보내 갱신하세요. UUID만 남고 토큰이 없으면 새 게스트 세션을 발급받아야 합니다.",
                    examples = {
                            @ExampleObject(name = "로그인 사용자", value = "Bearer <발급받은 AccessToken>"),
                            @ExampleObject(name = "비로그인 게스트", value = "Bearer <발급받은 guestToken>")
                    })
            @RequestHeader(value = AUTH_HEADER, required = false) String token,
            @Parameter(deprecated = true,
                    description = "하위 호환용 UUID query parameter이며 신규 요청에는 사용하지 마세요. " +
                            "모바일 게스트는 Authorization의 guestToken만 보내고, 브라우저 게스트는 guest_session 쿠키만 사용합니다. " +
                            "서버가 토큰 또는 쿠키에서 UUID와 사용자를 확인합니다.",
                    examples = {
                            @ExampleObject(name = "로그인 사용자", value = ""),
                            @ExampleObject(name = "비로그인 게스트", value = "123e4567-e89b-12d3-a456-426614174000")
                    })
            @RequestParam(value = "uuid", required = false) String uuid,
            @Parameter(
                    name = GUEST_SESSION_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "브라우저 게스트 세션 쿠키. 브라우저 게스트는 Auth 서비스에서 발급받으며 UUID와 Authorization 헤더는 보내지 않습니다."
            )
            @CookieValue(value = GUEST_SESSION_COOKIE, required = false) String guestSessionId) {
        Long loginedUserId;
        if (uuid != null) {
            if (uuid.isBlank() || token == null || token.isBlank()) {
                throw unauthorized("게스트 UUID와 게스트 토큰이 필요합니다.");
            }

            try {
                AuthGuestResponse guestUser = chatService.isValidGuestToken(token, uuid);
                if (guestUser == null || guestUser.getUserId() == null) {
                    throw unauthorized("유효하지 않은 게스트 토큰 또는 UUID입니다.");
                }
                loginedUserId = guestUser.getUserId();
            } catch (IllegalArgumentException e) {
                throw unauthorized("유효하지 않은 게스트 토큰 또는 UUID입니다.");
            }
        } else if (token != null && !token.isBlank()) {
            loginedUserId = resolveUserIdFromToken(token);
        } else if (guestSessionId != null && !guestSessionId.isBlank()) {
            try {
                loginedUserId = commonService
                        .fetchGuestBrowserSessionFromAuthService(guestSessionId)
                        .getUserId();
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is4xxClientError()) {
                    throw e;
                }
                throw unauthorized("유효하지 않거나 만료된 게스트 브라우저 세션입니다.");
            }
        } else {
            throw unauthorized("AccessToken 또는 게스트 브라우저 세션 쿠키가 필요합니다.");
        }

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

    private ChatRoomInviteException unauthorized(String message) {
        return new ChatRoomInviteException(ApiStatusCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, message);
    }

    private Long resolveUserIdFromToken(String token) {
        GuestYnDto guestYnDto = chatRoomService.isGuestToken(token);
        if (guestYnDto != null && "Y".equals(guestYnDto.getIsGuestYn())) {
            try {
                AuthGuestResponse guestUser = chatService.isValidGuestToken(token);
                if (guestUser == null || guestUser.getUserId() == null) {
                    throw unauthorized("유효하지 않은 게스트 토큰입니다.");
                }
                return guestUser.getUserId();
            } catch (RestClientResponseException e) {
                if (!e.getStatusCode().is4xxClientError()) {
                    throw e;
                }
                throw unauthorized("유효하지 않은 게스트 토큰입니다.");
            } catch (IllegalArgumentException e) {
                throw unauthorized("유효하지 않은 게스트 토큰입니다.");
            }
        }

        try {
            AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
            if (authUserResponse == null || authUserResponse.getUserId() == null) {
                throw unauthorized("유효하지 않은 AccessToken입니다.");
            }
            return authUserResponse.getUserId();
        } catch (RestClientResponseException e) {
            if (!e.getStatusCode().is4xxClientError()) {
                throw e;
            }
            throw unauthorized("유효하지 않은 AccessToken입니다.");
        } catch (IllegalArgumentException e) {
            throw unauthorized("유효하지 않은 AccessToken입니다.");
        }
    }

    @Operation(
            summary = "채팅방 입장 (채팅 내역 조회)",
            description = "로그인 사용자는 AccessToken, 모바일 게스트는 guestToken, 브라우저 게스트는 guest_session HttpOnly 쿠키로 인증해 채팅 내역을 조회합니다. " +
                    "UUID는 토큰 또는 쿠키에서 서버가 확인하므로 요청 URL과 본문에 넣지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 입장 및 채팅 내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = EnterChatRoomResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인 토큰 또는 게스트 브라우저 세션 쿠키가 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류 (토큰 인증 실패, 잘못된 roomId 등)",
                    content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
    })
    @GetMapping("/roomId={roomId}")
    public ResponseEntity<CommonApiResponse<EnterChatRoomResponseDto>> enterChatRoom(
            @PathVariable("roomId") Long roomId,
            HttpServletRequest request,
            @RequestHeader(value = AUTH_HEADER, required = false) String token,
            @Parameter(
                    name = GUEST_SESSION_COOKIE,
                    in = ParameterIn.COOKIE,
                    description = "브라우저 게스트 세션 쿠키"
            )
            @CookieValue(value = GUEST_SESSION_COOKIE, required = false) String guestSessionId) {
        log.info("roomId: {}", roomId);
        Long loginedUserId;
        if (token != null && !token.isBlank()) {
            loginedUserId = resolveUserIdFromToken(token);
        } else if (guestSessionId != null && !guestSessionId.isBlank()) {
            try {
                loginedUserId = commonService.fetchGuestBrowserSessionFromAuthService(guestSessionId).getUserId();
            } catch (RestClientResponseException exception) {
                if (!exception.getStatusCode().is4xxClientError()) {
                    throw exception;
                }
                throw unauthorized("유효하지 않거나 만료된 게스트 브라우저 세션입니다.");
            }
        } else {
            throw unauthorized("AccessToken 또는 게스트 브라우저 세션 쿠키가 필요합니다.");
        }
        List<Chat> chatList = chatService.getChatList(roomId, loginedUserId);

        EnterChatRoomResponseDto responseDto = EnterChatRoomResponseDto.builder()
                .loginedUserId(loginedUserId)
                .chatList(chatList)
                .build();

        ResponseEntity<CommonApiResponse<EnterChatRoomResponseDto>> response = ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(CommonApiResponse.<EnterChatRoomResponseDto>builder()
                .statusCode(ApiStatusCode.SUCCESS.getCode())
                .data(responseDto)
                .message("채팅방 입장 성공")
                .build());
        return response;
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
            description = "Authorization 헤더의 사용자 또는 기존 게스트 토큰으로 요청자를 확인합니다. 브라우저 게스트는 guest_session HttpOnly 쿠키로 인증할 수 있으며, 이 경우 UUID와 Authorization 헤더는 필요하지 않습니다. " +
                    "요청 본문의 필수 roomId 채팅방 참여자만 채팅 내역을 조회할 수 있으며, " +
                    "성공하면 채팅별 작성자 userId와 username을 포함한 목록을 반환하고 읽지 않은 메시지를 읽음 처리합니다. " +
                    "Authorization과 브라우저 쿠키가 모두 없으면 모바일/기존 클라이언트용 게스트 세션을 발급하고 응답의 uuId, guestToken, refreshToken을 반환합니다. " +
                    "refreshToken은 갱신 전용 비밀값이므로 안전한 저장소에 보관하고 일반 채팅 요청에 보내지 마세요. " +
                    "해당 사용자 프로필이 없거나 username이 null이면 username도 null로 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "채팅방 입장 및 채팅 내역 조회 성공",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class),
                            examples = @ExampleObject(
                                    name = "Authorization이 없는 신규 게스트",
                                    value = "{\"statusCode\":\"2030\",\"statusMsg\":\"success\",\"markedReadCount\":0,\"uuId\":\"123e4567-e89b-12d3-a456-426614174000\",\"guestToken\":\"<guestToken>\",\"refreshToken\":\"<refreshToken>\",\"guestTokenExpiresInSeconds\":1800,\"refreshTokenExpiresAt\":\"<ISO-8601 timestamp>\",\"isRefreshed\":\"N\"}"))),
            @ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
                    content = @Content(schema = @Schema(implementation = EnterRoomResponseDto.class))),
            @ApiResponse(responseCode = "401", description = "AccessToken, guestToken 또는 게스트 브라우저 세션 쿠키가 없거나 유효하지 않음",
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
            String browserSessionId = guestBrowserSessionId(request);
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                String token = authorizationHeader.substring("Bearer ".length());
                Long userId;
                AuthGuestResponse guestAuthResponse = null;
                GuestYnDto guestYnDto = chatRoomService.isGuestToken(token);
                if (guestYnDto != null && "Y".equals(guestYnDto.getIsGuestYn())) {
                    try {
                        guestAuthResponse = chatService.isValidGuestToken(token);
                    } catch (RestClientResponseException e) {
                        if (!e.getStatusCode().is4xxClientError()) {
                            throw e;
                        }
                        throw unauthorized("유효하지 않은 게스트 토큰입니다.");
                    } catch (IllegalArgumentException e) {
                        throw unauthorized("유효하지 않은 게스트 토큰입니다.");
                    }
                    if (guestAuthResponse == null || guestAuthResponse.getUserId() == null) {
                        throw unauthorized("유효하지 않은 게스트 토큰입니다.");
                    }
                    userId = guestAuthResponse.getUserId();
                } else {
                    try {
                        AuthUserResponse authUserResponse = chatService.isValidAccessToken(token);
                        if (authUserResponse == null || authUserResponse.getUserId() == null) {
                            throw unauthorized("유효하지 않은 AccessToken입니다.");
                        }
                        userId = authUserResponse.getUserId();
                    } catch (RestClientResponseException e) {
                        if (!e.getStatusCode().is4xxClientError()) {
                            throw e;
                        }
                        throw unauthorized("유효하지 않은 AccessToken입니다.");
                    } catch (IllegalArgumentException e) {
                        throw unauthorized("유효하지 않은 AccessToken입니다.");
                    }
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
                    responseDto.setIsRefreshed(guestAuthResponse.getIsRefreshed());
                }
                return ResponseEntity.status(ApiStatusCode.SUCCESS.getCode().equals(responseDto.getStatusCode())
                                ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(responseDto);
            } else if (browserSessionId != null) {
                GuestBrowserSessionDto guest;
                try {
                    guest = commonService.fetchGuestBrowserSessionFromAuthService(browserSessionId);
                } catch (RestClientResponseException exception) {
                    if (!exception.getStatusCode().is4xxClientError()) {
                        throw exception;
                    }
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(EnterRoomResponseDto.builder()
                            .statusCode(ApiStatusCode.UNAUTHORIZED.getCode())
                            .statusMsg("유효하지 않거나 만료된 게스트 브라우저 세션입니다.")
                            .markedReadCount(0)
                            .build());
                }
                Long userId = guest.getUserId();
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
                    responseDto.setChatList(chatService.getChatList(requestDto.getRoomId(), userId)
                            .stream()
                            .map(ChatResponseDto::fromEntity)
                            .toList());
                }
                return ResponseEntity.status(ApiStatusCode.SUCCESS.getCode().equals(responseDto.getStatusCode())
                                ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR)
                        .header(HttpHeaders.CACHE_CONTROL, "no-store")
                        .body(responseDto);
            } else {
                ChatGuestTokenInfo guestTokenInfo = chatRoomService.generateGuestSession();
                if (guestTokenInfo != null) {
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CACHE_CONTROL, "no-store")
                            .body(EnterRoomResponseDto.builder()
                                    .statusCode(ApiStatusCode.GUEST_TOKEN_ISSUED.getCode())
                                    .statusMsg("success")
                                    .markedReadCount(0)
                                    .guestToken(guestTokenInfo.getGuestToken())
                                    .refreshToken(guestTokenInfo.getRefreshToken())
                                    .guestTokenExpiresInSeconds(guestTokenInfo.getGuestTokenExpiresInSeconds())
                                    .refreshTokenExpiresAt(guestTokenInfo.getRefreshTokenExpiresAt())
                                    .uuId(guestTokenInfo.getUuId())
                                    .isRefreshed("N")
                                    .build());
                }
                throw new IllegalStateException("게스트 토큰을 발급하지 못했습니다.");
            }
        } catch (ChatRoomInviteException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(EnterRoomResponseDto.builder()
                    .statusCode(e.getApiStatusCode().getCode())
                    .statusMsg(e.getMessage())
                    .markedReadCount(0)
                    .build());
        } catch (Exception e) {
            log.error("채팅방 입장 처리 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(EnterRoomResponseDto.builder()
                    .statusCode(ApiStatusCode.INTERNAL_SERVER_ERROR.getCode())
                    .statusMsg("fail")
                    .markedReadCount(0)
                    .build());
        }
    }

    private String guestBrowserSessionId(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (jakarta.servlet.http.Cookie cookie : cookies) {
            if (GUEST_SESSION_COOKIE.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }
        return null;
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
