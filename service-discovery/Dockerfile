# 1단계: 빌드 스테이지
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app

# 해당 서비스의 gradle 파일들과 소스 코드만 복사
COPY service-discovery/gradlew ./gradlew
COPY service-discovery/gradle ./gradle
COPY service-discovery/build.gradle ./build.gradle
COPY service-discovery/settings.gradle ./settings.gradle
COPY service-discovery/src ./src

# 빌드 수행 (-Pprofile=dev 파라미터 추가, 테스트 제외)
RUN chmod +x ./gradlew
RUN ./gradlew bootJar -Pprofile=dev -x test --no-daemon

# 2단계: 실행 스테이지
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev

# 유레카 서버 포트 개방
EXPOSE 8761

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]