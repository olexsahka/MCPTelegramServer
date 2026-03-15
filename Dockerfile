# Stage 1: build
FROM gradle:8.7-jdk21 AS build
WORKDIR /app
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src
RUN gradle bootJar -x test --no-daemon

# Stage 2: run
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# TDLib data directory
RUN mkdir -p /app/tdlib-data

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
