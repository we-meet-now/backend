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
nohup java -Xms256m -Xmx512m -jar auth-service-0.0.1-SNAPSHOT.jar --server.port=6112 > /wasapp/logfs/auth-service/auth-service.log 2>&1 &
nohup java -Xms256m -Xmx512m -jar chat-service-0.0.1-SNAPSHOT.jar --server.port=6113 > /wasapp/logfs/chat-service/chat-service.log 2>&1 &
```

# Render & Railway MySQL 배포 및 연동 가이드

Render에서 Spring Boot 애플리케이션을 배포하고, Railway에서 제공하는 MySQL 데이터베이스와 안전하게 연동하는 과정에서 발생한 트러블슈팅과 최종 해결 방법을 정리한 가이드입니다.

---

## 1. 문제 상황 (Issue)
Render에 Spring Boot 애플리케이션(`auth-service`) 배포 시, 애플리케이션 초기 구동 단계에서 다음과 같은 에러가 발생하며 배포가 실패했습니다.
* **에러 메시지:** `java.lang.NullPointerException` 및 `Communications link failure` (SQL Error: 0, SQLState: 08S01)
* **원인:** Spring Boot가 구동되면서 DDL 실행 또는 데이터베이스 검증을 위해 Railway MySQL에 연결을 시도했으나, 네트워크 연결(Connection)을 수립하지 못함.

---

## 2. 주요 원인 분석

### ① 보안 정보 노출 위험 (Hardcoded Credentials)
초기 설정 파일(`application-db.properties`)에 데이터베이스 접속 주소, 비밀번호, JWT Secret 키가 평문(Raw Text)으로 기록되어 있어 GitHub에 그대로 Push될 경우 심각한 보안 위협이 있었습니다.

### ② 드라이버 암호화 통신 옵션 누락
Railway MySQL은 외부 환경에서 접속할 때 암호화 통신 방지 완화 파라미터(`allowPublicKeyRetrieval=true&useSSL=false`)가 URL 뒤에 붙지 않으면, 보안 메커니즘으로 인해 외부 컨테이너(Render)의 접속 패킷을 거부하여 `Communications link failure`를 유발합니다.

### ③ 빌드 시점(Docker Build) 환경 변수 주입 한계
스프링 속성 파일에 `${DB_URL}`과 같이 플레이스홀더를 사용하더라도, Render의 **Docker 빌드(Jar 생성) 시점**에 해당 환경 변수가 인식되지 않으면 기본값(`localhost:3306`)으로 Jar 파일이 빌드되어 버립니다. 이로 인해 런타임에 대시보드 환경 변수가 무시되는 현상이 발생했습니다.

---

## 3. 최종 해결 및 조치 단계 (Solution)

### 단계 1: Spring Boot 설정 파일 변수화 (`application-db.properties`)
실제 중요한 값들을 제거하고, 시스템 환경 변수를 주입받도록 구조를 변경했습니다. 값이 없을 경우를 대비한 로컬 기본값(`:기본값`)도 함께 정의했습니다.

```properties
# MySQL 8 드라이버 설정
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# 외부 환경 변수가 있으면 주입받고, 없으면 로컬 값 사용
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/wemeetnow?serverTimezone=Asia/Seoul}
spring.datasource.username=${DB_USER:root}
spring.datasource.password=${DB_PASSWORD:Qwe123!!}

# JWT 설정 변수화
jwt.secret=${JWT_SECRET:default_local_secret_key_at_least_32_bytes_long!!}