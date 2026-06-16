# backend

# CI/CD (GitHub Actions)

## 구조
- `dev` 브랜치에 push 시 자동 빌드 & EC2 배포
- `auth-service/` 변경 시 → auth-service 만 배포
- `chat-service/` 변경 시 → chat-service 만 배포

## GitHub Secrets 설정 (Repository → Settings → Secrets → Actions)

| Secret 이름 | 설명 | 예시 |
|---|---|---|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 | `172.31.45.0` |
| `EC2_USERNAME` | EC2 SSH 접속 계정 | `ubuntu` |
| `EC2_SSH_KEY` | EC2 접속용 PEM 키 내용 (전체) | `-----BEGIN RSA PRIVATE KEY-----...` |
| `EC2_PORT` | SSH 포트 | `22` |

### EC2 SSH Key 등록 방법
```bash
# 로컬에서 PEM 파일 내용 전체 복사
cat your-key.pem
# 복사한 내용을 EC2_SSH_KEY 값으로 등록
```

## EC2 서버 사전 준비
```bash
# EC2 서버에서 한 번만 실행``
mkdir -p /wasapp/transfer
mkdir -p /wasapp/logfs/auth-service
mkdir -p /wasapp/logfs/chat-service
```

## 배포 흐름
```
dev 브랜치 push
  └── GitHub Actions 트리거
        ├── JDK 17 설치
        ├── Gradle 빌드 (-Pprofile=dev)
        ├── JAR → EC2 /wasapp/transfer/ 전송 (SCP)
        └── EC2 SSH 접속
              ├── 기존 프로세스 종료 (포트 기준)
              ├── 기존 JAR 백업 (.bak)
              ├── 새 JAR 배포
              ├── nohup java -jar 실행
              └── 포트 기동 확인 (최대 60초)
```

## 워크플로우 파일 위치
- `.github/workflows/deploy-auth-service-dev.yml` (포트 6112)
- `.github/workflows/deploy-chat-service-dev.yml` (포트 6113)

# Build 방법
```cmd
$ ./gradlew clean build
```

# 공통 HTTP API 응답 클래스

- 모든 API 응답은 `CommonApiResponse<T>` 클래스를 통해 일관된 구조로 반환됩니다.
- 구조:
  - `statusCode`: String (상태 코드)
  - `data`: T (실제 데이터, DTO/Map/List 등)
  - `message`: String (설명 메시지)
- 예시:
```java
CommonApiResponse<MyDto> response = CommonApiResponse.<MyDto>builder()
    .statusCode("2000")
    .data(myDto)
    .message("성공")
    .build();
```

# 전역 예외 처리

- `@RestControllerAdvice`와 `@ExceptionHandler`를 활용하여 전역적으로 예외를 처리합니다.
- 예외 발생 시, 일관된 에러 응답(JSON)과 로그가 남습니다.
- 구현 위치: `com.wemeetnow.chat_service.exception.GlobalExceptionHandler`
- 예시:
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception occurred", ex); // 전체 스택트레이스 로그
        CommonApiResponse<Void> response = CommonApiResponse.<Void>builder()
                .statusCode("5005")
                .data(null)
                .message(ex.getMessage() != null ? ex.getMessage() : "서버 오류가 발생했습니다.")
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
```

# 직접 실행
```bash
cd /wasapp
nohup java -jar  auth-service-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
nohup java -jar  chat-service-0.0.1-SNAPSHOT.jar > /dev/null 2>&1 &
```

