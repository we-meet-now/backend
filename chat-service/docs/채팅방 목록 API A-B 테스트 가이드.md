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
  -Method Get `
  -Uri "$authApi/api/auth/v1/users/no-login/get-guest-token"
$guestUuidA = $guestA.data.uuId
$guestTokenA = $guestA.data.guestToken

$guestB = Invoke-RestMethod `
  -Method Get `
  -Uri "$authApi/api/auth/v1/users/no-login/get-guest-token"
$guestUuidB = $guestB.data.uuId
$guestTokenB = $guestB.data.guestToken
```

각 쌍은 반드시 같은 응답에서 받은 UUID와 guestToken을 함께 사용하세요. A의 UUID와 B의 guestToken을 섞으면 안 됩니다.

### UUID만 가지고 guestToken을 만들 수 있나요?

기존 게스트 UUID가 있으면 `POST /api/auth/v1/users/no-login/get-guest-token-by-uuid`로 그 UUID에 대한 guestToken을 발급할 수 있습니다. 요청 body는 UUID 하나이며 Authorization은 필요하지 않습니다. UUID는 게스트 인증 자격 증명이므로, 이 API를 호출할 수 있는 사람은 해당 UUID의 guestToken을 발급받을 수 있습니다. UUID를 비밀번호처럼 안전하게 보관하세요.

이 API는 기존 UUID가 실제 사용자로 등록되어 있는지 확인하고 해당 사용자에 대한 guestToken을 반환합니다. 새 게스트 사용자를 생성하거나 UUID를 새로 만들지는 않습니다. 등록되지 않은 UUID는 `404`입니다.

요청 입력 예시는 다음과 같습니다. UUID는 예시값이 아니라 DB에 등록된 실제 게스트 UUID로 바꿔야 합니다.

| 항목 | 값 |
|---|---|
| HTTP method/path | `POST /api/auth/v1/users/no-login/get-guest-token-by-uuid` |
| Request body | `{"uuid":"<기존 게스트 UUID>"}` |
| Authorization | 불필요 |
| Content-Type | `application/json` |

요청 예시:

```json
{
  "uuid": "123e4567-e89b-12d3-a456-426614174000"
}
```

발급 성공 응답에는 요청한 UUID와 새 guestToken이 포함됩니다.

```json
{
  "statusCode": "2000",
  "data": {
    "uuId": "123e4567-e89b-12d3-a456-426614174000",
    "guestToken": "<발급된 JWT>"
  },
  "message": "UUID로 게스트 토큰 발급 성공"
}
```

실제 응답의 `data.uuId`는 요청 UUID와 같아야 합니다. Chat API를 호출할 때는 `Authorization: Bearer <data.guestToken>`과 `?uuid=<data.uuId>`를 함께 사용합니다. Chat API의 쿼리 파라미터 이름은 소문자 `uuid`입니다.

PowerShell 예시:

```powershell
$existingUuid = '123e4567-e89b-12d3-a456-426614174000'
$guestTokenRequest = @{ uuid = $existingUuid } | ConvertTo-Json
$guestCredential = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/no-login/get-guest-token-by-uuid" `
  -ContentType 'application/json' `
  -Body $guestTokenRequest

$guestCredential.data.uuId
$guestCredential.data.guestToken
```

Swagger UI에서는 `http://localhost:6112/api/auth/v1/swagger-ui.html`을 열고 `POST /api/auth/v1/users/no-login/get-guest-token-by-uuid`의 **Try it out**을 누른 뒤 `{"uuid":"<기존 게스트 UUID>"}`를 입력해 실행하면 됩니다. Swagger의 UUID 예시값은 형식 안내용이므로 실제 등록 UUID로 교체하세요.

### 기존 UUID의 guestToken 갱신

기존 guestToken을 함께 보유하고 있으면 Auth 서비스의 `POST /api/auth/v1/users/get-guest-id`로 토큰을 검증하고 사용자 ID/이름을 확인할 수 있습니다. guestToken이 만료된 경우에도 기존 토큰에서 UUID를 확인할 수 있으면 새 guestToken을 발급합니다. 이 검증/갱신 API는 Authorization의 guestToken과 body의 UUID가 일치해야 합니다. guestToken을 분실하고 UUID만 남은 경우에는 앞 절의 `/no-login/get-guest-token-by-uuid`를 사용하세요.

Swagger UI를 사용할 경우 `http://localhost:6112/api/auth/v1/swagger-ui.html`을 열고 `POST /api/auth/v1/users/get-guest-id`에서 **Try it out**을 누릅니다. Authorization 헤더와 JSON request body에 아래 값을 입력한 뒤 Execute를 누르세요.

예상 입력:

```http
POST /api/auth/v1/users/get-guest-id
Authorization: Bearer <기존 guestToken>
Content-Type: application/json
```

```json
{
  "uuid": "123e4567-e89b-12d3-a456-426614174000"
}
```

`uuid`에는 Authorization의 guestToken으로부터 확인되는 UUID와 정확히 같은 값을 넣어야 합니다. PowerShell 예시:

```powershell
$refreshBody = @{ uuid = $guestUuidA } | ConvertTo-Json
$refreshHeaders = @{ Authorization = "Bearer $guestTokenA" }
$guestInfo = Invoke-RestMethod `
  -Method Post `
  -Uri "$authApi/api/auth/v1/users/get-guest-id" `
  -Headers $refreshHeaders `
  -ContentType 'application/json' `
  -Body $refreshBody

$guestInfo.data.uuId
$guestInfo.data.guestToken
$guestInfo.data.isRefreshed
```

기대 결과는 HTTP `200`, `statusCode: "2000"`입니다. `data.isRefreshed`가 `"N"`이면 기존 guestToken이 아직 유효하여 그대로 반환된 것이고, `"Y"`이면 만료된 기존 토큰을 검증하고 새 guestToken을 발급한 것입니다. 이후 Chat API에는 응답의 `data.uuId`와 `data.guestToken`을 사용하세요. UUID와 token이 일치하지 않으면 인증 실패 응답을 받습니다.

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

#### UUID는 있지만 guestToken이 없는 경우

1. Auth 서비스 Swagger UI `http://localhost:6112/api/auth/v1/swagger-ui.html`을 엽니다.
2. `POST /api/auth/v1/users/no-login/get-guest-token-by-uuid`에서 **Try it out**을 누릅니다.
3. Authorization은 비워 두고 Request Body에 기존 UUID를 입력합니다.

   ```json
   {
     "uuid": "123e4567-e89b-12d3-a456-426614174000"
   }
   ```

   UUID는 형식 참고용이므로 실제 등록되어 있는 UUID로 교체하세요. 성공 응답의 `data.guestToken`은 새 토큰이며, `data.uuId`는 요청 UUID와 같아야 합니다. UUID를 아는 사람은 토큰을 발급할 수 있으므로 공유하지 마세요.

4. 아래 Chat API 요청의 Authorization에는 응답의 `data.guestToken`을, `uuid`에는 같은 응답의 `data.uuId`를 입력합니다.

#### 새 게스트로 실행

1. Auth 서비스 Swagger UI에서 `GET /api/auth/v1/users/no-login/get-guest-token`을 실행합니다.
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
