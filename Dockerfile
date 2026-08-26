FROM gradle:8.14.1-jdk17 AS builder
WORKDIR /workspace
COPY build.gradle settings.gradle gradle.properties ./
COPY src ./src
RUN gradle --no-daemon clean bootJar

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S banking && adduser -S banking -G banking
WORKDIR /app
COPY --from=builder /workspace/build/libs/core-banking-1.0.0.jar app.jar
USER banking
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
