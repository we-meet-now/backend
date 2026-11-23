# 채팅방 목록 API A/B 테스트 가이드

## 1. 테스트 대상과 인증 규칙

테스트 대상은 `GET /api/chat/v1/chat-rooms`입니다. 로그인 사용자와 비로그인 게스트가 같은 API를 사용합니다.

| 사용자 유형 | 요청 값 | 처리 방식 |
|---|---|---|
| 로그인 사용자 | `Authorization: Bearer <AccessToken>`, `uuid` 생략 | Auth 서비스에서 AccessToken을 검증하고 사용자 ID로 목록 조회 |
| 비로그인 게스트 | `Authorization: Bearer <guestToken>` 및 `?uuid=<uuid>` | Auth 서비스에서 guestToken과 UUID가 한 쌍인지 검증한 뒤 목록 조회 |

`uuid`가 있으면 게스트 방식으로 처리합니다. **UUID만 보내는 요청은 허용하지 않습니다.** guestToken과 UUID가 서로 다르거나 누락되면 `401`입니다. `uuid` 없이 guestToken만 보내면 AccessToken으로 처리되어 인증에 실패합니다.

테스트 주소는 로컬 기본 포트 기준입니다.

- Auth 서비스: `http://localhost:6112`
- Chat 서비스: `http://localhost:6113`

실행 환경에서 포트나 API Gateway 주소가 다르면 해당 주소로 바꿔 요청하세요.

## 2. A/B 계정과 테스트 데이터 준비

로그인 A와 B에는 **서로 다른 개발/QA 테스트 계정**을 사용합니다. 아래 이메일은 형식 예시일 뿐, 실제 계정이나 비밀번호가 아닙니다.

| 구분 | 이메일 예시 | 비밀번호 |
|---|---|---|
| 로그인 A | `chat-list-a@example.test` | 로컬에서 안전하게 입력 |
| 로그인 B | `chat-list-b@example.test` | 로컬에서 안전하게 입력 |

두 계정에 연결된 채팅방 참여 데이터가 있어야 A/B 목록 차이를 확인할 수 있습니다. 기존 데이터가 없다면 개발/QA 환경에서 테스트 계정과 참여 데이터를 준비하세요. 운영 계정이나 운영 DB를 사용하지 말고, DB를 직접 변경하지 마세요. MCP PostgreSQL 도구는 읽기 전용입니다.

목록이 비어 있어도 인증에 성공하면 API 응답은 `200`일 수 있습니다. 사용자 분리까지 확인하려면 A와 B가 서로 다른 참여 채팅방을 하나 이상 갖도록 테스트 데이터를 준비하는 것이 좋습니다.

### DB에서 계정 및 참여 채팅방 확인

아래 쿼리는 사용자 비밀번호를 읽지 않습니다. 이메일 예시를 실제 개발/QA 테스트 계정 이메일로 바꾸세요.

```sql
SELECT
    u.user_id,
    u.email,
    u.uuid,
    cp.chat_room_id,
    cp.use_yn,
    cr.chat_room_nm
FROM public."user" u
LEFT JOIN chat_participant cp ON cp.user_id = u.user_id
LEFT JOIN chat_room cr ON cr.chat_room_id = cp.chat_room_id
WHERE u.email IN ('chat-list-a@example.test', 'chat-list-b@example.test')
ORDER BY u.email, cp.chat_room_id;
```

- 결과의 `user_id`로 각 계정의 요청 응답 `data.loginedUserId`를 대조합니다.
- `chat_room_id`로 응답의 `data.chatRoomList[].chatRoom.chatRoomId`를 대조합니다.
- 현재 채팅방 목록 조회 쿼리는 `chat_participant.user_id`로 연결하며 `use_yn`을 필터링하지 않습니다. DB 대조 시에도 `use_yn = 'Y'` 조건을 임의로 추가하지 마세요.
- 조회 결과에 참여 채팅방이 없으면 성공 응답의 `chatRoomList`가 빈 배열일 수 있습니다.

게스트 토큰 발급 후 UUID가 어느 사용자 ID에 연결됐는지 확인하려면 다음 쿼리를 사용할 수 있습니다. `<guest-uuid-A>` 등을 응답에서 받은 값으로 바꾸세요.

```sql
SELECT user_id, uuid
FROM public."user"
WHERE uuid IN ('<guest-uuid-A>', '<guest-uuid-B>');
```

refreshToken 원문은 DB에 저장하지 않습니다. 게스트 세션의 만료/폐기 상태만 확인하려면 다음 쿼리를 사용하세요.

```sql
SELECT
    gs.user_id,
    u.uuid,
    gs.created_at,
    gs.expires_at,
    gs.absolute_expires_at,
    gs.last_used_at,
    gs.revoked_at
FROM guest_session gs
JOIN public."user" u ON u.user_id = gs.user_id
WHERE u.uuid IN ('<guest-uuid-A>', '<guest-uuid-B>');
```

운영 배포 전에는 `auth-service/docs/guest_session_postgresql.sql`을 실행해 `guest_session` 테이블을 생성하세요. 현재 `prod1` 프로필은 Hibernate DDL 자동 갱신을 활성화하지 않았습니다.

응답의 게스트 UUID와 guestToken은 모두 인증 정보입니다. 실제 값을 문서, 티켓, 채팅방에 복사해 공유하지 마세요.

### 미읽음 메시지 수 DB 대조

응답의 각 채팅방 `isNotReadCount`는 해당 사용자 ID가 읽음 처리하지 않은 채팅 수와 대조할 수 있습니다. `<room-id>`와 `<user-id>`를 실제 응답/DB 값으로 바꾸세요.

```sql
SELECT COUNT(c.chat_id) AS is_not_read_count
FROM chat c
WHERE c.chat_room_id = <room-id>
  AND c.chat_id NOT IN (
      SELECT cr.chat_id
      FROM chat_read cr
      WHERE cr.user_id = <user-id>
  );
```

## 3. 로그인 AccessToken 발급

다음 예시는 PowerShell 기준입니다. 이메일은 위에서 준비한 개발/QA 테스트 계정으로 바꾸고, 비밀번호는 `Get-Credential` 프롬프트에 입력하세요. 비밀번호를 스크립트나 문서에 저장하지 마세요.

```powershell
$authApi = 'http://localhost:6112'
$chatApi = 'http://localhost:6113'

$credentialA = Get-Credential -UserName 'chat-list-a@example.test'
$loginA = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/login" `
  -ContentType 'application/json' `
  -Body (@{
    email = $credentialA.UserName
    password = $credentialA.GetNetworkCredential().Password
  } | ConvertTo-Json)
$accessTokenA = $loginA.data.accessToken

$credentialB = Get-Credential -UserName 'chat-list-b@example.test'
$loginB = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/login" `
  -ContentType 'application/json' `
  -Body (@{
    email = $credentialB.UserName
    password = $credentialB.GetNetworkCredential().Password
  } | ConvertTo-Json)
$accessTokenB = $loginB.data.accessToken
```

로그인이 성공하면 응답의 `data.accessToken`을 사용합니다. 로그인 API 자체가 실패하면 먼저 계정/비밀번호 및 Auth 서비스 상태를 확인하세요.

## 4. 게스트 UUID와 guestToken 발급

게스트 A와 B는 guest token 발급 API를 각각 호출해 별개의 UUID/token 쌍을 받습니다.

```powershell
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

각 쌍은 반드시 같은 응답에서 받은 UUID와 guestToken을 함께 사용하세요. A의 UUID와 B의 guestToken을 섞으면 안 됩니다.

### 게스트 세션 생성, 전환 및 갱신

신규 클라이언트는 Auth 서비스의 `POST /api/auth/v1/users/no-login/guest-sessions`를 호출합니다. 이 API는 `data.refreshToken`을 응답 본문으로 반환하므로, 브라우저는 가능하면 BFF가 HttpOnly·Secure 쿠키로 보관하게 하고 모바일은 Keychain/Keystore 같은 보안 저장소에 보관하세요. `data.uuId`와 `data.guestToken`은 Chat API 요청에 함께 사용하며 refreshToken은 일반 채팅 요청에 보내지 않습니다.

성공 시 HTTP `201`, `statusCode: "2000"`이며 `data.guestTokenExpiresInSeconds`는 1800입니다. 응답은 `Cache-Control: no-store`로 반환됩니다.
기존 `POST /api/auth/v1/users/get-guest-id`는 guestToken 검증/사용자 정보 조회용이며 만료 토큰을 갱신하지 않습니다.

기존 게스트가 이전 발급 API의 UUID와 아직 유효한 guestToken을 가지고 있다면 아래 전환 API를 한 번 호출해 refreshToken을 등록합니다. guestToken이 만료된 뒤에는 전환할 수 없으므로 기존 게스트를 복구할 수 없습니다.

```http
POST /api/auth/v1/users/no-login/guest-sessions/upgrade
Authorization: Bearer <기존 guestToken>
Content-Type: application/json
```

```json
{
  "uuid": "123e4567-e89b-12d3-a456-426614174000"
}
```

만료되지 않은 guestToken의 UUID와 body의 UUID가 일치하면 응답의 `data.refreshToken`을 안전하게 보관합니다. UUID만으로는 전환하거나 토큰을 발급받을 수 없습니다.

### guestToken 만료 시 갱신

guestToken은 30분 동안 유효합니다. 보관한 refreshToken을 다음 요청 body로 Auth 서비스에 보내세요. Authorization 헤더나 UUID는 보내지 않습니다. Backend는 연결된 UUID를 DB에서 찾고, 성공 시 기존 refreshToken을 무효화한 뒤 새 guestToken과 새 refreshToken을 반환합니다. refreshToken은 30일 미사용 또는 발급 후 최대 180일이면 만료됩니다.

```http
POST /api/auth/v1/users/no-login/guest-sessions/refresh
Content-Type: application/json
```

```json
{
  "refreshToken": "<현재 저장된 refreshToken>"
}
```

PowerShell 예시:

```powershell
$refreshBody = @{ refreshToken = $guestRefreshTokenA } | ConvertTo-Json
$refreshedSession = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/no-login/guest-sessions/refresh" `
  -ContentType 'application/json' `
  -Body $refreshBody

$guestUuidA = $refreshedSession.data.uuId
$guestTokenA = $refreshedSession.data.guestToken
$guestRefreshTokenA = $refreshedSession.data.refreshToken
```

Refresh API는 한 번 사용한 refreshToken을 즉시 교체합니다. 여러 요청에서 동시에 갱신하지 말고, 응답을 안전하게 저장한 뒤 새 값을 사용하세요. 유효하지 않거나 만료된 refreshToken은 HTTP `401`이며, UUID만으로 복구할 수 없습니다. 둘 다 잃었다면 새 게스트 세션을 발급받아야 하며 기존 채팅방은 연결되지 않습니다.

갱신 성공은 HTTP `200`, `statusCode: "2000"`이며 `data.guestToken`과 `data.refreshToken` 모두 새 값입니다. 반드시 새 refreshToken을 기존 값 대신 저장하세요.

Swagger UI에서는 `POST /api/auth/v1/users/no-login/guest-sessions`를 실행해 첫 세션을 발급하고, `POST /api/auth/v1/users/no-login/guest-sessions/refresh`에 저장한 refreshToken을 입력해 갱신을 확인할 수 있습니다. 실제 토큰을 화면 캡처나 공유 문서에 남기지 마세요.

## 5. 성공 요청

### A — 로그인 사용자 목록

```powershell
$headersA = @{ Authorization = "Bearer $accessTokenA" }
$roomsA = Invoke-RestMethod `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms" `
  -Headers $headersA
$roomsA | ConvertTo-Json -Depth 10
```

기대 결과:

- HTTP 상태: `200 OK`
- `statusCode`: `"2000"`
- `message`: `"채팅방 목록 조회 성공"`
- `data.loginedUserId`: DB에서 확인한 로그인 A의 `user_id`
- `data.chatRoomList`: A의 `user_id`로 연결된 채팅방 목록. 참여 채팅방이 없으면 `[]`

### B — 로그인 사용자 목록

```powershell
$headersB = @{ Authorization = "Bearer $accessTokenB" }
$roomsB = Invoke-RestMethod `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms" `
  -Headers $headersB
$roomsB | ConvertTo-Json -Depth 10
```

A와 같은 형식으로 `data.loginedUserId`가 B의 `user_id`인지 확인하고, 채팅방 ID를 DB 결과와 대조하세요. A/B가 서로 다른 방에만 참여한다면 상대방 전용 방이 응답에 섞이지 않아야 합니다. 공통 참여 방은 양쪽 응답에 모두 나타날 수 있습니다.

### 게스트 A — UUID와 guestToken

```powershell
$guestHeadersA = @{ Authorization = "Bearer $guestTokenA" }
$guestUriA = "$chatApi/api/chat/v1/chat-rooms?uuid=$([uri]::EscapeDataString($guestUuidA))"
$guestRoomsA = Invoke-RestMethod `
  -Method Get `
  -Uri $guestUriA `
  -Headers $guestHeadersA
$guestRoomsA | ConvertTo-Json -Depth 10
```

게스트 B도 B의 UUID/token 쌍으로 같은 요청을 보냅니다.

```powershell
$guestHeadersB = @{ Authorization = "Bearer $guestTokenB" }
$guestUriB = "$chatApi/api/chat/v1/chat-rooms?uuid=$([uri]::EscapeDataString($guestUuidB))"
$guestRoomsB = Invoke-RestMethod `
  -Method Get `
  -Uri $guestUriB `
  -Headers $guestHeadersB
$guestRoomsB | ConvertTo-Json -Depth 10
```

기대 결과는 로그인 요청과 동일한 성공 응답 형식입니다. `data.loginedUserId`는 DB에서 해당 UUID로 조회한 `user_id`와 같아야 하고, `chatRoomList`는 그 ID의 참여 채팅방에 한정되어야 합니다.

### 성공 응답 예시

아래는 구조 예시입니다. 채팅방 필드는 실제 데이터에 따라 달라집니다.

```json
{
  "statusCode": "2000",
  "data": {
    "loginedUserId": 123,
    "chatRoomList": [
      {
        "chatRoom": {
          "chatRoomId": 501,
          "chatRoomNm": "A 테스트 모임"
        },
        "isNotReadCount": 2
      }
    ]
  },
  "message": "채팅방 목록 조회 성공"
}
```

`isNotReadCount`는 해당 채팅방/사용자 쌍에 대한 미읽음 수 SQL 결과와 대조하세요. 응답의 `chatRoom`은 채팅방 엔티티를 포함하므로 테스트 응답을 외부에 공유할 때는 민감한 필드를 제거하세요.

## 6. 실패 요청과 기대 결과

### UUID만 전달 — 실패

guestToken 없이 UUID만 전달합니다.

```powershell
Invoke-RestMethod `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms?uuid=$([uri]::EscapeDataString($guestUuidA))"
```

기대 결과: HTTP `401 Unauthorized`, `statusCode` `"4010"`, 메시지 `"게스트 UUID와 게스트 토큰이 필요합니다."`

### Authorization과 UUID 모두 누락 — 실패

```powershell
Invoke-WebRequest -Method Get -Uri "$chatApi/api/chat/v1/chat-rooms"
```

기대 결과: HTTP `401 Unauthorized`, `statusCode` `"4010"`, 메시지 `"Authorization AccessToken이 필요합니다."`

### 서로 다른 게스트 UUID/token 조합 — 실패

```powershell
$wrongPairHeaders = @{ Authorization = "Bearer $guestTokenB" }
$wrongPairUri = "$chatApi/api/chat/v1/chat-rooms?uuid=$([uri]::EscapeDataString($guestUuidA))"
Invoke-WebRequest -Method Get -Uri $wrongPairUri -Headers $wrongPairHeaders
```

기대 결과: HTTP `401 Unauthorized`, `statusCode` `"4010"`, 메시지 `"유효하지 않은 게스트 토큰 또는 UUID입니다."`

### 잘못된 AccessToken — 실패

```powershell
$invalidHeaders = @{ Authorization = 'Bearer invalid-token-for-test' }
Invoke-WebRequest `
  -Method Get `
  -Uri "$chatApi/api/chat/v1/chat-rooms" `
  -Headers $invalidHeaders
```

기대 결과: HTTP `401 Unauthorized`, `statusCode` `"4010"`, 메시지 `"유효하지 않은 AccessToken입니다."`

실패 응답 예시:

```json
{
  "statusCode": "4010",
  "data": null,
  "message": "유효하지 않은 게스트 토큰 또는 UUID입니다."
}
```

PowerShell의 `Invoke-WebRequest`/`Invoke-RestMethod`는 4xx 응답에서 예외를 발생시킬 수 있습니다. 응답 본문까지 확인하려면 다음처럼 `ErrorDetails.Message`를 확인하세요.

```powershell
try {
  Invoke-RestMethod -Method Get -Uri "$chatApi/api/chat/v1/chat-rooms"
} catch {
  $_.Exception.Response.StatusCode
  $_.ErrorDetails.Message
}
```

Auth 서비스가 응답하지 않거나 서버 내부 오류가 발생하면 `500`일 수 있습니다. 이는 인증 실패의 `401`과 구분하여 Auth 서비스 연결/로그를 확인하세요.

## 7. A/B 결과 확인표

| 확인 항목 | A 요청 | B 요청 | 성공 기준 |
|---|---|---|---|
| 로그인 식별 | AccessToken A | AccessToken B | `loginedUserId`가 각각 DB의 A/B `user_id`와 일치 |
| 게스트 식별 | UUID A + guestToken A | UUID B + guestToken B | `loginedUserId`가 각 UUID의 DB `user_id`와 일치 |
| 방 목록 분리 | A의 결과 | B의 결과 | 각 결과의 방 ID가 해당 사용자 `chat_participant` 행과 일치 |
| 미읽음 수 | 응답별 `isNotReadCount` | 응답별 `isNotReadCount` | `chat`/`chat_read` 대조 쿼리 결과와 일치 |
| 인증 실패 | UUID/token 불일치 또는 값 누락 | 잘못된 AccessToken | HTTP `401`, `statusCode` `"4010"` |

## 8. Swagger UI에서 로그인/게스트 직접 테스트

1. Chat 서비스 Swagger UI를 엽니다: `http://localhost:6113/api/chat/v1/swagger-ui.html`
2. `채팅방 API`에서 `GET /api/chat/v1/chat-rooms`를 펼친 뒤 **Try it out**을 누릅니다.
3. Authorization과 `uuid` 입력란의 예시는 형식 안내용 placeholder입니다. 그대로 실행하지 말고 아래 방법으로 발급한 실제 값을 붙여 넣으세요.

### 로그인 사용자로 실행

1. Auth 서비스 Swagger UI `http://localhost:6112/api/auth/v1/swagger-ui.html`에서 `POST /api/auth/v1/users/login`을 실행합니다.
2. Request body에 개발/QA 테스트 계정 이메일과 비밀번호를 입력합니다.

   ```json
   {
     "email": "chat-list-a@example.test",
     "password": "<테스트 계정 비밀번호>"
   }
   ```

3. 성공 응답의 `data.accessToken`을 복사합니다.
4. Chat 서비스 API의 Authorization 입력란에 `Bearer <복사한 accessToken>`을 입력합니다.
5. `uuid`는 비워 둡니다. 요청 URL에 `?uuid=`가 붙지 않았는지 확인한 다음 **Execute**를 누릅니다.
6. HTTP `200`, `statusCode: "2000"`, `data.loginedUserId`, `data.chatRoomList`를 확인합니다.

로그인 B도 B 계정으로 로그인한 뒤 같은 단계를 반복합니다. 실제 토큰을 화면 캡처나 공유 문서에 남기지 마세요.

### 비로그인 게스트로 실행

#### UUID만 남고 guestToken을 분실한 경우

UUID만으로 기존 게스트의 guestToken을 복구할 수 없습니다. 기존 채팅 기록을 유지해야 한다면 UUID와 함께 보관한 guestToken을 찾아야 합니다. 두 값을 모두 분실했거나 토큰을 복구할 수 없다면 Auth 서비스 Swagger UI에서 `POST /api/auth/v1/users/no-login/guest-sessions`을 실행해 새 UUID/token 쌍을 발급받으세요. 새 UUID는 새 게스트 identity이며 기존 채팅방을 조회할 수 없습니다.

#### 새 게스트로 실행

1. Auth 서비스 Swagger UI에서 `POST /api/auth/v1/users/no-login/guest-sessions`을 실행합니다.
2. 한 번의 응답에서 `data.uuId`와 `data.guestToken`을 복사합니다. 두 값은 인증 쌍이므로 다른 발급 응답의 값을 섞지 마세요.
3. Chat 서비스 API의 Authorization 입력란에 `Bearer <복사한 guestToken>`을 입력합니다.
4. `uuid` 입력란에는 같은 응답의 `data.uuId`를 입력하고 **Execute**를 누릅니다.
5. 요청 URL이 `GET /api/chat/v1/chat-rooms?uuid=<발급받은-UUID>` 형태인지 확인합니다.
6. HTTP `200`, `statusCode: "2000"`, 응답의 `data.loginedUserId`와 DB의 UUID 매핑 결과를 대조합니다.

게스트 B를 테스트할 때는 B의 UUID에 대해 guestToken을 발급받고 두 값을 함께 사용합니다. Chat API에는 여전히 UUID와 guestToken이 모두 필요하며, UUID만 직접 보내면 `401`입니다.

### Swagger UI에서 실패 케이스 확인

- **UUID만 입력하고 Authorization을 비움:** HTTP `401`, `statusCode: "4010"`, 게스트 UUID와 토큰이 필요하다는 오류가 나와야 합니다.
- **게스트 A UUID와 게스트 B token을 조합:** HTTP `401`, `statusCode: "4010"`, UUID/token 불일치 오류가 나와야 합니다.
- **무작위 AccessToken 입력 후 uuid는 비움:** HTTP `401`, `statusCode: "4010"`, 유효하지 않은 AccessToken 오류가 나와야 합니다.
- **로그인 AccessToken을 입력하고 uuid도 입력:** UUID가 존재하면 게스트 방식으로 분기되므로 HTTP `401`이어야 합니다.

401 응답도 Swagger UI의 **Server response**에 표시됩니다. 응답 본문에서 `statusCode`와 `message`를 확인하고, 요청 URL 및 Authorization에 A/B의 서로 다른 값이 들어갔는지 확인하세요. Auth 서비스와의 연결 자체에 실패하거나 서버 오류가 발생하면 HTTP `500`일 수 있으므로, 이 경우에는 토큰 불일치와 구분해 각 서비스 로그를 확인합니다.
