# Repository Instructions

## Windows Test Worktrees

- On Windows, when creating a Git worktree to test a commit or branch separately, create the worktree on the same drive as the project being tested. For example, if the project is on `D:`, put the test worktree on `D:` as well; do not use the default `C:` temporary directory for the worktree.

## Spring API Development

For new or materially refactored API endpoints:

- Follow the response contract in `.copilot-rules.md` and return `CommonApiResponse<T>`.
- Document each endpoint with Springdoc `@Operation` and `@ApiResponses`. Write a clear Korean summary and description covering the endpoint purpose, authentication, required request data, and important success/error cases.
- In `auth-service` controllers, inject the `JwtUtil` bean and call `jwtUtil.getAuthenticatedUser(HttpServletRequest)` to authenticate the caller and resolve the persisted `User`. Use that user's ID for caller-specific operations; do not parse the Authorization header in controllers, trust a caller ID from the request, or hardcode a user ID.
- Protect authenticated endpoints explicitly in `SecurityConfig`. Put the endpoint matcher before broader `permitAll` matchers.
- Use typed request and response DTOs. Validate required values and return the appropriate `ErrorCode` through `CustomException`; rely on `GlobalExceptionHandler` for the common error response.
- Add or update focused tests for successful behavior and relevant validation, authorization, and not-found cases.

### `auth-service` User Access Logging

- For user-facing `auth-service` API methods whose access should be audited, wrap the operation with `UserLogService.executeWithLogging(...)`. It records returned HTTP statuses and thrown failures, measures duration, and writes the row through `UserLogPersistenceService` in a separate transaction.
- Pass the canonical endpoint path as `menuLink`, a `Supplier<String>` that resolves the persisted `User.user_id`, the operation, and a status resolver that returns the actual HTTP status (for example, `response -> response.getStatusCode().value()`).
- Never store the submitted email, username, request-supplied ID, password, token, or a hardcoded value in `inp_user_id`. Resolve the ID from the persisted `User`; return `null` when no persisted user exists, such as an unknown-account login attempt. Keep `UserLog.inpUserId` nullable for that case.
- Do not log credentials, tokens, or raw request bodies. `UserLogService` fills request method, IP, request ID, user agent, request URI, device type, status, and duration.
- Use `UserLogService.recordUserLog(...)` when an operation cannot be wrapped, supplying the resolved persisted user ID, endpoint path, HTTP status, and elapsed duration.
- Add focused tests for both successful and failed operations, including the resolved `inp_user_id` and response status.

For services outside `auth-service`, use that service's existing authentication and API response conventions rather than assuming its JWT implementation is shared.

### `chat-service` User Identity

- When a `chat-service` API needs the authenticated user's ID, call `CommonService.fetchUserFromAuthService(authorizationHeader)` and use the returned `AuthUserDto.userId`.
- Do not fetch the user ID directly from `auth-service`, decode the JWT in controllers, accept a caller-supplied user ID as the authenticated identity, or duplicate this lookup in another service. Keep Auth-service user lookups centralized in `CommonService`.
- Pass the incoming Authorization header to `CommonService`; it normalizes the authorization scheme and validates the wrapped Auth-service response.

### `chat-service`, `auth-service` Swagger Documentation

- When creating or changing a `chat-service` or `auth-service` API, update its Springdoc/Swagger documentation in the same change so the documented request, response, authentication requirements, and success/error cases match the implementation.
- Document endpoints with `@Operation` and `@ApiResponses`, using a clear Korean summary and description. For example:

```java
@Operation(
        summary = "전체 채팅 참여자 테이블 조회",
        description = "로그인 없이 모든 채팅 참여자 행과 전체 컬럼을 조회합니다."
)
@ApiResponses({
        @ApiResponse(responseCode = "200", description = "참여자 테이블 조회 성공",
                content = @Content(schema = @Schema(implementation = CommonApiResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
                content = @Content(schema = @Schema(implementation = CommonApiResponse.class)))
})
```
