# Enter Code 생성 기능 - 구현 완료

## 개요
채팅방 생성 시 입장코드(enterCode)를 자동으로 생성합니다.
- **형식**: MMDD + 4자리 랜덤 숫자 (총 8자리)
- **예시**: 09271234 (2026년 9월 27일에 생성된 코드)

## 구현 내용

### 1. 새로 생성된 테이블: enter_code_pool
날짜별로 생성된 4자리 숫자를 관리하여 중복 방지

**테이블 구조:**
```
pool_id          : PRIMARY KEY (자동생성)
code_date        : 코드 생성 날짜
random_code      : 4자리 랜덤 숫자
use_yn           : 사용 여부 (Y/N)
inp_date         : 생성일시
mdfy_date        : 수정일시
inp_user_id      : 생성 사용자
mdfy_user_id     : 수정 사용자
```

### 2. 수정된 테이블: enter_code_mgmt
이미 존재하는 테이블에 chatRoomId를 명시적으로 설정하여 ChatRoom과의 관계 정의

**테이블 구조:**
```
chat_room_id     : PRIMARY KEY (ChatRoom과의 외래키)
enter_code       : 입장코드 (MMDD + 4자리)
inp_date         : 생성일시
mdfy_date        : 수정일시
inp_user_id      : 생성 사용자
mdfy_user_id     : 수정 사용자
```

### 3. 수정된 테이블: chat_room
enterCode 필드에 생성된 코드가 저장됨

## 생성된 파일들

### Domain
- `EnterCodePool.java` - 4자리 숫자 관리용 도메인

### Repository
- `EnterCodeMgmtRepository.java` - EnterCodeMgmt 조회 (이전에 없던 저장소)
- `EnterCodePoolRepository.java` - EnterCodePool 조회 및 저장

### Service
- `EnterCodeGenerator.java` - 입장코드 생성 서비스
  - `generateEnterCode(Long chatRoomId)`: enterCode 생성 및 저장

### Modified Service
- `ChatRoomService.java`
  - `createOnChatRoom()`: enterCode 생성 추가
  - `createAnonymousChatRoom()`: enterCode 생성 추가

## 동작 흐름

1. **채팅방 생성 요청**
   ```
   POST /api/chat/v1/chat-rooms/create-one
   ```

2. **ChatRoomService.createOnChatRoom() 실행**
   - ChatRoom 엔티티 생성 및 저장 (enterCode는 null)
   - chatRoomId 획득

3. **EnterCodeGenerator.generateEnterCode(chatRoomId) 호출**
   - 현재 날짜 조회 (예: 0927)
   - EnterCodePool에서 오늘 날짜의 사용된 4자리 코드 조회
   - 중복되지 않는 4자리 랜덤 숫자 생성
   - EnterCodePool에 새로운 레코드 저장 (use_yn='Y')
   - EnterCodeMgmt에 새로운 레코드 저장 (chatRoomId + enterCode)
   - 생성된 enterCode 반환

4. **ChatRoom 업데이트**
   - ChatRoom의 enterCode 필드에 생성된 코드 저장

5. **응답**
   - 채팅방 ID와 입장코드를 포함한 응답 반환

## 테스트 방법

### 1. API 호출 예시

**요청:**
```bash
curl -X POST http://localhost:6113/api/chat/v1/chat-rooms/create-one \
  -H "Authorization: Bearer {JWT_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "chatRoomNm": "테스트 채팅방",
    "participantIds": [1, 2, 3]
  }'
```

**응답:**
```json
{
  "statusCode": "2001",
  "data": {
    "chatRoomId": 123
  },
  "message": "채팅방 생성 성공"
}
```

### 2. 데이터베이스 확인

**EnterCodePool 테이블 조회:**
```sql
SELECT * FROM enter_code_pool 
WHERE code_date = CURRENT_DATE 
ORDER BY pool_id DESC LIMIT 10;
```

**EnterCodeMgmt 테이블 조회:**
```sql
SELECT * FROM enter_code_mgmt 
WHERE enter_code LIKE TO_CHAR(CURRENT_DATE, 'MMDD') || '%' 
ORDER BY chat_room_id DESC LIMIT 10;
```

**ChatRoom 테이블 조회:**
```sql
SELECT chat_room_id, chat_room_nm, enter_code 
FROM chat_room 
WHERE enter_code IS NOT NULL 
ORDER BY chat_room_id DESC LIMIT 10;
```

### 3. 예상 결과

같은 날짜에 3개의 채팅방을 생성했을 때:

| chat_room_id | enter_code |
|--------------|-----------|
| 1            | 09271234  |
| 2            | 09275678  |
| 3            | 09279012  |

EnterCodeMgmt 테이블:
| chat_room_id | enter_code |
|--------------|-----------|
| 1            | 09271234  |
| 2            | 09275678  |
| 3            | 09279012  |

EnterCodePool 테이블:
| pool_id | code_date  | random_code | use_yn |
|---------|-----------|-------------|--------|
| 1       | 2026-09-27| 1234        | Y      |
| 2       | 2026-09-27| 5678        | Y      |
| 3       | 2026-09-27| 9012        | Y      |

## 기술 사항

### 동시성 처리
- `@Transactional` 애노테이션으로 트랜잭션 관리
- 데이터베이스 레벨의 UNIQUE 제약조건으로 추가 보호
  ```sql
  UNIQUE (code_date, random_code)
  ```

### 에러 처리
- 10000번 시도 후에도 중복되지 않는 숫자를 생성하지 못하면 예외 발생
  ```
  IllegalStateException: "Failed to generate unique random code after 100 attempts"
  ```

### 로깅
- 생성된 enterCode와 chatRoomId는 로그에 기록됨
  ```
  INFO: Generated enterCode: 09271234 for chatRoomId: 1
  DEBUG: Saved to EnterCodePool - date: 2026-09-27, randomCode: 1234
  DEBUG: Saved to EnterCodeMgmt - chatRoomId: 1, enterCode: 09271234
  ```

## 주의사항

1. **여러 번의 저장**: ChatRoomService에서 ChatRoom을 2번 저장합니다 (enterCode 설정을 위해)
   - 이는 의도된 동작입니다
   - 첫 번째: chatRoomId 획득을 위해
   - 두 번째: enterCode 설정 후 최종 저장

2. **날짜 변경**: 날짜가 변경되면 4자리 숫자가 초기화됩니다
   - 2026-09-27: 1234 ~ 9999 사용 가능
   - 2026-09-28: 0000 ~ 9999 다시 사용 가능

3. **@CreatedBy 사용**: AuditingEntityListener가 활성화되어야 합니다
   - auth-service의 설정 확인 필요
   - 없으면 inp_user_id가 null이 될 수 있습니다
