FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY common common
COPY payment-service payment-service
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':payment-service'" > settings.gradle
RUN chmod +x gradlew && ./gradlew :payment-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/payment-service/build/libs/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
