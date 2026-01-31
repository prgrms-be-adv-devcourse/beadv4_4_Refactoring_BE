FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY common common
COPY product-service product-service
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':product-service'" > settings.gradle
RUN chmod +x gradlew && ./gradlew :product-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/product-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
