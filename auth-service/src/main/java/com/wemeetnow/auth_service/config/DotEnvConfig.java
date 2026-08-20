package com.wemeetnow.auth_service.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class DotEnvConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // 1. 프로파일 결정 (getProperty 우선 조회 -> activeProfiles -> 기본값 local)
        String profile = environment.getProperty("spring.profiles.active");
        if (profile == null || profile.isBlank()) {
            String[] activeProfiles = environment.getActiveProfiles();
            profile = (activeProfiles.length > 0) ? activeProfiles[0] : "local";
        }

        String filename = ".env." + profile;

        // 2. 인텔리제이 루트 디렉터리 / 모듈 디렉터리 경로 보정
        Path userDirPath = Paths.get(System.getProperty("user.dir"));
        Path envDirPath = userDirPath.endsWith("auth-service")
                ? userDirPath
                : userDirPath.resolve("auth-service");

        Path fullPath = envDirPath.resolve(filename);

        // 3. .env 파일이 존재하는 경우에만 로드
        if (Files.exists(fullPath)) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(envDirPath.toString())
                        .filename(filename)
                        .ignoreIfMissing()
                        .load();

                Map<String, Object> props = new HashMap<>();
                dotenv.entries().forEach(entry -> props.put(entry.getKey(), entry.getValue()));

                // Spring Environment 최우선 순위(addFirst)로 주입
                environment.getPropertySources().addFirst(new MapPropertySource("dotenvProperties", props));
                System.out.println("[DotEnvConfig] Successfully loaded " + props.size() + " properties from " + filename);
            } catch (Exception e) {
                System.err.println("[DotEnvConfig] Failed to load " + filename + ": " + e.getMessage());
            }
        } else {
            System.out.println("[DotEnvConfig] File not found, skipping: " + fullPath.toAbsolutePath());
        }
    }
}