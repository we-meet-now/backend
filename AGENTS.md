# Repository Instructions

## Spring API Development

For new or materially refactored API endpoints:

- Follow the response contract in `.copilot-rules.md` and return `CommonApiResponse<T>`.
- Document each endpoint with Springdoc `@Operation` and `@ApiResponses`. Write a clear Korean summary and description covering the endpoint purpose, authentication, required request data, and important success/error cases.
- In `auth-service` controllers, inject the `JwtUtil` bean and call `jwtUtil.getAuthenticatedUser(HttpServletRequest)` to authenticate the caller and resolve the persisted `User`. Use that user's ID for caller-specific operations; do not parse the Authorization header in controllers, trust a caller ID from the request, or hardcode a user ID.
- Protect authenticated endpoints explicitly in `SecurityConfig`. Put the endpoint matcher before broader `permitAll` matchers.
- Use typed request and response DTOs. Validate required values and return the appropriate `ErrorCode` through `CustomException`; rely on `GlobalExceptionHandler` for the common error response.
- Add or update focused tests for successful behavior and relevant validation, authorization, and not-found cases.

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
