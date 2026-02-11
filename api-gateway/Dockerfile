FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Gradle 파일 복사
COPY gradlew .
COPY gradle gradle

# common 및 api-gateway 모듈 복사
COPY common common
COPY api-gateway api-gateway

# settings.gradle 동적 생성 (필요한 모듈만 포함)
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':api-gateway'" > settings.gradle

# 빌드
RUN chmod +x gradlew && ./gradlew :api-gateway:bootJar -x test --no-daemon

# 런타임 이미지
FROM eclipse-temurin:17-jre
WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
