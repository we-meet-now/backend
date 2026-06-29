# 1단계: 빌드 스테이지 (JDK 포함 환경)
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 해당 서비스의 gradle 파일들과 소스 코드만 복사
COPY auth-service/gradlew ./gradlew
COPY auth-service/gradle ./gradle
COPY auth-service/build.gradle ./build.gradle
COPY auth-service/settings.gradle ./settings.gradle
COPY auth-service/src ./src

# 빌드 수행 (테스트는 제외)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -x test --no-daemon

# 2단계: 실행 스테이지 (실행에 필요한 JRE만 포함하여 용량 최소화)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Render 대시보드에서 주입받을 환경변수 설정
ENV SPRING_PROFILES_ACTIVE=dev

EXPOSE 6112
ENTRYPOINT ["sh", "-c", "java -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]