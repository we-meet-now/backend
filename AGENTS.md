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
