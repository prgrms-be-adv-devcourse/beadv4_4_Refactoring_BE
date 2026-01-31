FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY common common
COPY member-service member-service
RUN echo "rootProject.name = 'api'\ninclude ':common'\ninclude ':member-service'" > settings.gradle
RUN chmod +x gradlew && ./gradlew :member-service:bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/member-service/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
