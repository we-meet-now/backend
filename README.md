# backend

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
