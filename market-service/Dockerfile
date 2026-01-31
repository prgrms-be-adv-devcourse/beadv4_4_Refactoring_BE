FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY common common
COPY market-service market-service
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':market-service'" > settings.gradle
RUN chmod +x gradlew && ./gradlew :market-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/market-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
