FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY common common
COPY settlement-service settlement-service
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':settlement-service'" > settings.gradle
RUN chmod +x gradlew && ./gradlew :settlement-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/settlement-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
