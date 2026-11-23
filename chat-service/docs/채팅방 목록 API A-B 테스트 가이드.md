# 채팅방 목록 API A/B 테스트 가이드

## 1. 인증 규칙

`GET /api/chat/v1/chat-rooms`는 로그인 AccessToken, 모바일 게스트 `guestToken`, 또는 브라우저 `guest_session` 쿠키로 사용자를 식별합니다.

| 사용자 유형 | 요청 인증 | UUID 전달 |
|---|---|---|
| 로그인 사용자 | `Authorization: Bearer <accessToken>` | 보내지 않음 |
| 모바일 게스트 | `Authorization: Bearer <guestToken>` | 보내지 않음 |
| 브라우저 게스트 | HttpOnly `guest_session` 쿠키 | 보내지 않음 |

Auth 서버는 guestToken에 포함된 UUID를 persisted user와 대조해 검증합니다. Chat API의 URL, query parameter, request body에 UUID나 userId를 넣지 마세요. refreshToken은 Chat API가 아닌 Auth의 refresh endpoint에만 보냅니다.

아래 예시는 로컬 기본 주소 기준입니다.

- Auth: `http://localhost:6112`
- Chat: `http://localhost:6113`

개발/QA 환경에서 테스트하고, 토큰을 로그·문서·스크린샷에 남기지 마세요.

## 2. 게스트 A/B 자격증명 발급

다음 PowerShell 예시는 서로 다른 두 게스트를 발급합니다. `uuId`는 DB 대조용으로만 잠시 보관하며 Chat 요청에는 사용하지 않습니다. 네이티브 앱에서는 `guestToken`을 메모리에서 사용하고 `refreshToken`은 Keychain/Keystore에 보관하세요. 이번 작업에서는 모바일 저장 코드나 별도 API를 추가하지 않습니다.

```powershell
$authApi = 'http://localhost:6112'
$chatApi = 'http://localhost:6113'

$guestA = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/no-login/guest-sessions"
$guestUuidA = $guestA.data.uuId
$guestTokenA = $guestA.data.guestToken
$guestRefreshTokenA = $guestA.data.refreshToken

$guestB = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/no-login/guest-sessions"
$guestUuidB = $guestB.data.uuId
$guestTokenB = $guestB.data.guestToken
$guestRefreshTokenB = $guestB.data.refreshToken
```

성공 응답은 HTTP `201`, `statusCode: "2000"`입니다. 한 게스트의 refreshToken을 다른 게스트 요청에 사용하지 마세요.

## 3. guestToken만으로 Chat API 호출

```powershell
$headersA = @{ Authorization = "Bearer $guestTokenA" }
$roomsA = Invoke-RestMethod `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms" `
  -Headers $headersA

$headersB = @{ Authorization = "Bearer $guestTokenB" }
$roomsB = Invoke-RestMethod `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms" `
  -Headers $headersB

$roomsA | ConvertTo-Json -Depth 10
$roomsB | ConvertTo-Json -Depth 10
```

기대 결과는 각각 HTTP `200`, `statusCode: "2000"`입니다. `data.loginedUserId`가 Auth가 해당 guestToken에서 확인한 사용자 ID인지 확인하세요. 참여 채팅방이 없으면 `data.chatRoomList`는 빈 배열일 수 있습니다.

클라이언트 fetch 호출은 다음과 같습니다.

```javascript
const response = await fetch(`${chatApi}/api/chat/v1/chat-rooms`, {
  headers: {
    Authorization: `Bearer ${guestToken}`
  }
});

if (!response.ok) {
  throw new Error(`Chat API failed: ${response.status}`);
}

const rooms = await response.json();
```

다른 인증된 채팅방 API도 같은 규칙을 따릅니다. 예를 들어 `POST /api/chat/v1/chat-rooms/enter-room` 본문에는 참여 중인 방의 `roomId`만 보냅니다.

```json
{
  "roomId": 501
}
```

메시지 전송용 STOMP 연결은 CONNECT의 Authorization 헤더에 guestToken을 설정합니다. `chat.sendMessage`/`chat.addUser` 본문은 `chatRoomId`, `message`, `chatType`으로 구성하고 UUID와 userId는 포함하지 않습니다. 서버가 인증 Principal에서 사용자 ID를 가져옵니다.

브라우저 게스트는 Authorization 대신 같은 사이트 API Gateway의 HttpOnly 쿠키를 사용합니다.

```javascript
const response = await fetch(`${apiBase}/api/chat/v1/chat-rooms`, {
  credentials: "include"
});
```

브라우저의 도메인 구성, 쿠키 발급 및 Postman cookie jar 테스트는 [비로그인 게스트 자격증명 관리 가이드](비로그인_게스트_자격증명_관리_가이드.md)의 브라우저 세션 절을 참고하세요.

## 4. 인증 실패 테스트

Authorization 없이 쿠키도 없이 요청하면 HTTP `401`이어야 합니다.

```powershell
try {
  Invoke-RestMethod -Method Get -Uri "$chatApi/api/chat/v1/chat-rooms"
} catch {
  $_.Exception.Response.StatusCode
  $_.ErrorDetails.Message
}
```

잘못되거나 만료된 guestToken도 HTTP `401`이어야 합니다.

```powershell
$invalidHeaders = @{ Authorization = 'Bearer invalid-guest-token' }
try {
  Invoke-RestMethod `
    -Method Get `
    -Uri "$chatApi/api/chat/v1/chat-rooms" `
    -Headers $invalidHeaders
} catch {
  $_.Exception.Response.StatusCode
  $_.ErrorDetails.Message
}
```

성공적인 A/B 테스트에서는 각 요청의 채팅방 목록이 해당 사용자에게 연결된 참여 데이터와 일치해야 합니다. 권한이 없는 사용자의 채팅방이 다른 요청 결과에 포함되지 않는지 확인하세요.

## 5. UUID와 사용자 ID DB 대조 (선택)

발급 응답의 UUID는 DB 검증에만 사용하고 SQL 결과를 공유할 때 실제 UUID는 마스킹하세요.

```sql
SELECT user_id, uuid
FROM public."user"
WHERE uuid IN ('<guest-uuid-A>', '<guest-uuid-B>');
```

운영 데이터베이스 대신 개발/QA DB를 사용하세요.
