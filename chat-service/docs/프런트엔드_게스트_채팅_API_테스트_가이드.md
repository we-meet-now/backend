# 프런트엔드 게스트 채팅 API 테스트 가이드

이 문서는 게스트 사용자가 UUID나 userId를 Chat API 요청에 보내지 않고, guestToken 또는 브라우저 세션 쿠키로 인증되는 흐름을 프런트엔드에서 확인하는 방법을 설명합니다.

## 1. 테스트 환경과 사전 조건

로컬 기본 주소는 다음과 같습니다.

| 구분 | 주소 |
|---|---|
| 프런트 개발 서버 | `http://localhost:5173` |
| Auth | `http://localhost:6112` |
| Chat | `http://localhost:6113` |

프런트엔드에서 테스트할 때 공통 주소를 한 번 선언하고 아래 예시를 실행하세요.

```javascript
const authBase = "http://localhost:6112";
const chatBase = "http://localhost:6113";
```

Auth와 Chat이 실행 중이어야 합니다. 채팅방 입장 테스트에는 해당 게스트가 참여자로 등록된 `roomId`가 필요합니다. 예시의 `501`은 실제 DB의 참여 중인 방 ID로 바꾸세요.

현재 프런트의 `front-azure-iota.vercel.app`과 Auth/Chat의 Render 주소를 각각 직접 호출하는 경우, 브라우저 `guest_session` 쿠키는 두 Render 호스트 간에 공유되지 않습니다. 운영 환경에서 쿠키 인증을 테스트하려면 프런트와 같은 사이트에 둔 API Gateway의 단일 호스트를 통해 Auth와 Chat을 호출해야 합니다. 토큰 인증은 guestToken을 Authorization 헤더에 전달하는 방식으로 별도 테스트할 수 있습니다.

## 2. 모바일/토큰 방식: 게스트 토큰 발급

테스트용 게스트 자격증명은 Auth API에서 발급합니다. 이 요청에는 Authorization 헤더와 request body가 필요하지 않습니다.

```http
POST http://localhost:6112/api/auth/v1/users/no-login/guest-sessions
```

프런트엔드 예시:

```javascript
const createResponse = await fetch(
  `${authBase}/api/auth/v1/users/no-login/guest-sessions`,
  { method: "POST" }
);

if (!createResponse.ok) {
  throw new Error(`Guest session creation failed: ${createResponse.status}`);
}

const createResult = await createResponse.json();
const { uuId, guestToken, refreshToken } = createResult.data;

// 테스트 중 guestToken은 메모리에서 사용합니다.
// refreshToken은 refresh API에만 보내고, 일반 Chat 요청에는 보내지 않습니다.
// uuId는 Chat 요청에 포함하지 않습니다.
```

성공 응답은 HTTP `201`이며, 주요 응답 형태는 다음과 같습니다. 실제 토큰 값은 로그, URL, 분석 이벤트, 스크린샷에 남기지 마세요.

```json
{
  "statusCode": "2000",
  "data": {
    "uuId": "<발급된 UUID>",
    "guestToken": "<발급된 guestToken>",
    "refreshToken": "<발급된 refreshToken>",
    "guestTokenExpiresInSeconds": 1800,
    "refreshTokenExpiresAt": "<ISO-8601 timestamp>"
  },
  "message": "게스트 세션 생성 성공"
}
```

발급 응답에서 UUID를 확인할 수는 있지만, 다음 Chat API 호출의 URL, query string, request body에는 복사하지 않습니다. 이 가이드의 토큰 인증 예시는 Chat 요청에 `guestToken`만 사용합니다.

## 3. 내 채팅방 목록 조회

`GET /api/chat/v1/chat-rooms`는 request body 없이 호출합니다.

```http
GET http://localhost:6113/api/chat/v1/chat-rooms
Authorization: Bearer <guestToken>
```

```javascript
const roomsResponse = await fetch(`${chatBase}/api/chat/v1/chat-rooms`, {
  method: "GET",
  headers: {
    Authorization: `Bearer ${guestToken}`
  }
});

if (!roomsResponse.ok) {
  throw new Error(`Chat room list failed: ${roomsResponse.status}`);
}

const roomsResult = await roomsResponse.json();
console.log(roomsResult.data.chatRoomList);
```

성공은 HTTP `200`, `statusCode: "2000"`입니다. 응답의 `data.loginedUserId`는 서버가 토큰으로 확인한 사용자 ID이고, `data.chatRoomList`에는 해당 사용자의 채팅방과 읽지 않은 메시지 수가 반환됩니다. 참여한 채팅방이 없으면 목록이 빈 배열일 수 있습니다.

요청 URL에 `?uuid=...`를 붙이지 마세요. 기존 UUID query parameter는 하위 호환용으로만 남아 있으며 deprecated 상태입니다.

## 4. 채팅 내역 조회 및 읽음 처리

### 채팅 내역 조회

`GET /api/chat/v1/chat-rooms/roomId={roomId}`는 body 없이 호출합니다.

```http
GET http://localhost:6113/api/chat/v1/chat-rooms/roomId=501
Authorization: Bearer <guestToken>
```

```javascript
const roomId = 501;
const historyResponse = await fetch(
  `${chatBase}/api/chat/v1/chat-rooms/roomId=${roomId}`,
  {
    headers: {
      Authorization: `Bearer ${guestToken}`
    }
  }
);

if (!historyResponse.ok) {
  throw new Error(`Chat history failed: ${historyResponse.status}`);
}

const historyResult = await historyResponse.json();
console.log(historyResult.data);
```

### 채팅방 입장 및 읽음 처리

`POST /api/chat/v1/chat-rooms/enter-room`의 JSON body에는 `roomId`만 보냅니다. Authorization에는 발급받은 guestToken을 사용합니다.

```http
POST http://localhost:6113/api/chat/v1/chat-rooms/enter-room
Authorization: Bearer <guestToken>
Content-Type: application/json

{
  "roomId": 501
}
```

```javascript
const enterResponse = await fetch(
  `${chatBase}/api/chat/v1/chat-rooms/enter-room`,
  {
    method: "POST",
    headers: {
      Authorization: `Bearer ${guestToken}`,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({ roomId })
  }
);

const enterResult = await enterResponse.json();
if (!enterResponse.ok) {
  throw new Error(`Enter room failed: ${enterResponse.status}`);
}

console.log(enterResult.chatList);
```

응답에는 `loginedUserId`, `markedReadCount`, `chatList` 등이 포함됩니다. 이 endpoint는 `CommonApiResponse` wrapper 없이 `EnterRoomResponseDto`를 직접 반환합니다. 요청 본문에 `uuid`나 `userId`를 추가하지 마세요. 게스트가 해당 방의 참여자가 아니면 HTTP `403`을 기대합니다.

## 5. 브라우저 방식: HttpOnly 쿠키로 호출

브라우저에서는 UUID, guestToken, refreshToken을 JavaScript에서 관리하지 않고 Auth가 발급한 `guest_session` 쿠키를 사용합니다. Auth 세션 생성 request body는 필요하지 않습니다.

### 세션 쿠키 생성

```http
POST http://localhost:6112/api/auth/v1/users/no-login/guest-browser-sessions
```

```javascript
const sessionResponse = await fetch(
  `${authBase}/api/auth/v1/users/no-login/guest-browser-sessions`,
  {
    method: "POST",
    credentials: "include"
  }
);

if (!sessionResponse.ok) {
  throw new Error(`Browser guest session creation failed: ${sessionResponse.status}`);
}

// 응답 JSON에 UUID, guestToken, refreshToken 또는 쿠키 원문은 없습니다.
const sessionResult = await sessionResponse.json();
console.log(sessionResult.data);
```

Auth 응답의 `Set-Cookie` 헤더로 `HttpOnly; Secure; SameSite=Lax; Path=/` 쿠키가 설정됩니다. JavaScript에서 쿠키 값을 읽지 말고, 다음 요청에도 `credentials: "include"`를 설정하세요.

### 쿠키로 목록 및 방 입장 호출

```javascript
const cookieRoomsResponse = await fetch(
  `${chatBase}/api/chat/v1/chat-rooms`,
  {
    method: "GET",
    credentials: "include"
  }
);

const cookieRooms = await cookieRoomsResponse.json();
if (!cookieRoomsResponse.ok) {
  throw new Error(`Cookie-authenticated room list failed: ${cookieRoomsResponse.status}`);
}

const cookieEnterResponse = await fetch(
  `${chatBase}/api/chat/v1/chat-rooms/enter-room`,
  {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ roomId: 501 })
  }
);

const cookieEnterResult = await cookieEnterResponse.json();
if (!cookieEnterResponse.ok) {
  throw new Error(`Cookie-authenticated room entry failed: ${cookieEnterResponse.status}`);
}
```

쿠키 인증 요청에는 Authorization 헤더, UUID query parameter, UUID/userId body field를 추가하지 않습니다. 로컬 브라우저에서는 Auth와 Chat 모두 `localhost`를 사용하고, credential을 포함한 CORS 요청이 허용되어야 합니다. 운영에서는 Auth와 Chat이 같은 API Gateway 호스트를 통해 제공되어야 합니다.

### 브라우저 세션 갱신

세션을 회전할 때는 다음 Auth API를 body 없이 호출합니다. 브라우저가 현재 쿠키를 보내고, 성공 시 새 쿠키를 저장합니다.

```http
POST http://localhost:6112/api/auth/v1/users/no-login/guest-browser-sessions/refresh
```

```javascript
await fetch(
  `${authBase}/api/auth/v1/users/no-login/guest-browser-sessions/refresh`,
  {
    method: "POST",
    credentials: "include"
  }
);
```

`/resolve`는 Chat 서버가 사용하는 내부 확인 API입니다. 프런트엔드에서 직접 호출하지 마세요.

## 6. 5단계: guestToken 만료 갱신과 이전 refreshToken 폐기 확인

guestToken이 만료되었지만 refreshToken이 유효하면 Auth refresh API를 호출합니다. 백엔드는 저장된 refreshToken hash와 요청 값을 비교하고, 성공 시 새 토큰 쌍을 발급해 DB hash를 교체합니다. 이 요청의 body는 `refreshToken` 하나이며, 이전 refreshToken은 즉시 무효화됩니다.

```http
POST http://localhost:6112/api/auth/v1/users/no-login/guest-sessions/refresh
Content-Type: application/json

{
  "refreshToken": "<현재 보유한 refreshToken>"
}
```

```javascript
const refreshResponse = await fetch(
  `${authBase}/api/auth/v1/users/no-login/guest-sessions/refresh`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken })
  }
);

if (!refreshResponse.ok) {
  throw new Error(`Guest token refresh failed: ${refreshResponse.status}`);
}

const refreshed = (await refreshResponse.json()).data;
// 앱의 보안 저장소에 새 두 값을 함께 저장하고 기존 값은 삭제합니다.
const newGuestToken = refreshed.guestToken;
const newRefreshToken = refreshed.refreshToken;
```

갱신 후 Chat 요청에는 새 guestToken을 사용하고, refreshToken은 일반 Chat API에 보내지 않습니다. 이전 값의 재사용도 확인하려면 갱신 전 refreshToken을 잠깐 보관한 뒤 다음 요청을 한 번 실행합니다. 이 재사용 요청은 HTTP `401`이어야 합니다.

```javascript
const replayResponse = await fetch(
  `${authBase}/api/auth/v1/users/no-login/guest-sessions/refresh`,
  {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken: previousRefreshToken })
  }
);

if (replayResponse.status !== 401) {
  throw new Error(`Expected stale refreshToken to be rejected, got ${replayResponse.status}`);
}
```

`previousRefreshToken`은 갱신 직전의 값이어야 합니다. 이 검증 후에는 항상 갱신 응답에서 받은 새 refreshToken만 사용하세요. 여러 탭이 동시에 갱신하지 않도록 프런트엔드에서 refresh 요청을 단일화해야 합니다.

## 7. STOMP 메시지 테스트

WebSocket 연결 주소는 `ws://localhost:6113/api/chat/v1/message/ws-chat`이며 서버는 SockJS 경로도 제공합니다. 구독 destination은 `/api/chat/v1/message/topic/public`입니다. 클라이언트에서 서버로 보내는 destination과 body 예시는 다음과 같습니다.

```text
SEND destination: /api/chat/v1/message/app/chat.sendMessage
Body:
{
  "chatRoomId": 501,
  "message": "프런트엔드 게스트 인증 테스트",
  "chatType": "CHAT"
}
```

메시지 전송 body의 허용 필드는 `chatRoomId`, `message`, `chatType`이며 `chatType`은 `CHAT`, `ENTER`, `LEAVE` 중 하나입니다. `userId`와 UUID를 보내지 않습니다. 서버는 WebSocket 연결의 인증 Principal에서 작성자 ID를 가져옵니다.

guestToken 인증 연결 예시 (`@stomp/stompjs`):

```javascript
import { Client } from "@stomp/stompjs";

const stompClient = new Client({
  brokerURL: "ws://localhost:6113/api/chat/v1/message/ws-chat",
  connectHeaders: {
    Authorization: `Bearer ${guestToken}`
  },
  onConnect: () => {
    stompClient.subscribe(
      "/api/chat/v1/message/topic/public",
      frame => console.log(JSON.parse(frame.body))
    );

    stompClient.publish({
      destination: "/api/chat/v1/message/app/chat.sendMessage",
      body: JSON.stringify({
        chatRoomId: 501,
        message: "프런트엔드 게스트 인증 테스트",
        chatType: "CHAT"
      })
    });
  }
});

stompClient.activate();
```

브라우저 쿠키 인증에서는 `connectHeaders.Authorization`을 생략합니다. 같은 사이트 Gateway를 통해 연결하면 브라우저가 handshake에 HttpOnly 쿠키를 자동 포함합니다. 구독과 메시지 body 형식은 토큰 인증 예시와 같습니다.

## 8. 확인 항목과 오류 테스트

- 브라우저 개발자 도구의 Network 탭에서 Chat 요청 URL에 `uuid` query parameter가 없는지 확인합니다.
- `enter-room` request body가 정확히 `{"roomId": 501}`인지 확인합니다.
- STOMP `SEND` body에 `userId`와 UUID가 없는지 확인합니다.
- 인증된 응답의 `loginedUserId`가 서버가 확인한 사용자 ID인지 확인합니다.
- 참여하지 않은 방에 입장하면 HTTP `403`, guestToken 또는 세션 쿠키가 잘못되거나 만료되면 HTTP `401`인지 확인합니다.
- `GET /api/chat/v1/chat-rooms`를 Authorization 헤더와 쿠키 없이 호출하면 HTTP `401`이어야 합니다.

주의: `POST /api/chat/v1/chat-rooms/enter-room`에 Authorization 헤더와 브라우저 쿠키가 모두 없으면 현재 서버가 신규 게스트 자격증명을 발급하는 호환 동작을 합니다. 인증 실패 동작을 검증할 때는 이 요청을 무인증으로 보내지 말고, 목록 GET의 무인증 요청이나 잘못된 토큰/쿠키를 사용하세요.
