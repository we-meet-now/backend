# 1단계: 빌드 스테이지
FROM openjdk:17-jdk-slim AS build
WORKDIR /app

# 소스 및 설정 파일 복사
COPY auth-service/gradlew ./gradlew
COPY auth-service/gradle ./gradle
COPY auth-service/build.gradle ./build.gradle
COPY auth-service/settings.gradle ./settings.gradle
COPY auth-service/src ./src

# 실행 권한 부여 및 빌드 (속도를 위해 테스트 제외)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 스테이지
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Render 대시보드에서 주입받을 환경변수(SPRING_PROFILES_ACTIVE)를 자바 실행 옵션에 연결
# 기본값은 dev로 설정하되, 외부 주입이 있으면 그 값을 따릅니다.
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 6112
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]