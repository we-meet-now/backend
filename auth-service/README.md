# Auth Service

사용자 인증 및 권한 관리를 담당하는 마이크로서비스입니다.

## 목차
- [개발 환경 설정](#개발-환경-설정)
  - [환경 변수 설정 (.env 파일)](#환경-변수-설정-env-파일)
- [실행 방법](#실행-방법)
- [API 문서](#api-문서)

---

## 개발 환경 설정

### 환경 변수 설정 (.env 파일)

이 프로젝트는 `dotenv-java` 라이브러리를 사용하여 환경별로 다른 환경 변수를 관리합니다.

#### 개요

- **Local 환경**: `.env.local` 파일에서 환경 변수 읽기
- **Dev 환경**: `.env.dev` 파일에서 환경 변수 읽기
- **Prod 환경**: `.env.prod` 파일에서 환경 변수 읽기

#### 파일 구조

```
auth-service/
├── .env.local    (Local 개발 환경)
├── .env.dev      (Dev 개발 환경)
├── .env.prod     (Prod 운영 환경)
└── src/main/resources-env/
    ├── local/
    │   └── application.properties (spring.profiles.active=local)
    ├── dev/
    │   └── application.properties (spring.profiles.active=dev)
    └── prod1/
        └── application.properties (spring.profiles.active=prod)
```

#### 환경 변수 예시

각 `.env.*` 파일에 다음과 같이 설정합니다:

```env
# .env.local / .env.dev / .env.prod
RESEND_API_KEY=your_resend_api_key_here
```

#### 동작 원리

```
1. 애플리케이션 시작
   ↓
2. DotEnvConfig 초기화 (설정 클래스)
   ↓
3. spring.profiles.active 값 읽기 (local / dev / prod)
   ↓
4. 해당 프로파일의 .env 파일 로드 (.env.local / .env.dev / .env.prod)
   ↓
5. 환경 변수를 Java 시스템 프로퍼티로 등록
   ↓
6. application.properties에서 ${RESEND_API_KEY} 형식으로 참조
```

#### DotEnvConfig 클래스

```java
@Configuration
public class DotEnvConfig {
    @Autowired
    public DotEnvConfig(Environment env) {
        String[] activeProfiles = env.getActiveProfiles();
        String profile = (activeProfiles.length > 0) ? activeProfiles[0] : "local";
        
        String filename = ".env." + profile;
        
        Dotenv dotenv = Dotenv.configure()
                .directory("./auth-service")
                .filename(filename)
                .ignoreIfMissing()
                .load();
        
        dotenv.entries().forEach(entry -> {
            System.setProperty(entry.getKey(), entry.getValue());
        });
    }
}
```

#### application-auth.properties

```properties
# Resend API from .env file
resend.api.key=${RESEND_API_KEY:}
resend.api.url=https://api.resend.com/emails
resend.sender.email=no-reply@wemeetnow.kr
resend.sender.name=wemeetnow
```

---

## 실행 방법

### Local 환경 (기본값)

```bash
./gradlew bootRun
```

자동으로 `spring.profiles.active=local`이 설정되고 `.env.local` 파일의 환경 변수를 로드합니다.

### Dev 환경

```bash
./gradlew bootRun -Pprofile=dev
```

또는 IDE 설정에서 Program arguments에 `-Pprofile=dev` 추가

### Prod 환경

```bash
./gradlew bootRun -Pprofile=prod
```

또는 IDE 설정에서 Program arguments에 `-Pprofile=prod` 추가

---

## API 문서

### Swagger UI

애플리케이션 실행 후 아래 주소에서 API 문서를 확인할 수 있습니다:

- **Local**: http://localhost:6112/api/auth/v1/swagger-ui.html
- **Dev**: https://backend-auth-service-new.onrender.com/api/auth/v1/swagger-ui.html

### 주요 API 엔드포인트

#### 사용자 관리
- `POST /api/auth/v1/users/join` - 회원가입
- `POST /api/auth/v1/users/login` - 로그인
- `GET /api/auth/v1/users/get-id` - AccessToken에서 userId 조회
- `GET /api/auth/v1/users/get-user-info` - 사용자 상세 정보 조회
- `PUT /api/auth/v1/users/update-info` - 사용자 정보 수정
- `PUT /api/auth/v1/users/update-address` - 사용자 주소 정보 업데이트

#### 이메일 인증
- `POST /api/auth/v1/users/email/send-verification` - 이메일 인증 코드 전송
- `POST /api/auth/v1/users/email/verify` - 이메일 인증 코드 검증

---

## 기술 스택

- **Framework**: Spring Boot 3.3.3
- **Language**: Java 17
- **Build Tool**: Gradle
- **Database**: PostgreSQL
- **Authentication**: JWT (JSON Web Token)
- **Mail Service**: Resend API
- **API Documentation**: SpringDoc OpenAPI (Swagger UI)
- **Environment Management**: dotenv-java

---

## 주의 사항

- `.env.local`, `.env.dev`, `.env.prod` 파일은 `.gitignore`에 추가되어야 합니다 (민감한 정보 보호)
- 환경 변수가 없으면 `${RESEND_API_KEY:}` 형식으로 기본값(빈 문자열)을 설정하여 오류를 방지합니다
- 프로덕션 배포 시 환경 변수는 시스템 환경 변수나 클라우드 플랫폼의 환경 설정에서 관리하는 것을 권장합니다
