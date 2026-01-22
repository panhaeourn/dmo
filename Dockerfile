# -------- Build stage --------
FROM gradle:8.10-jdk21 AS build
WORKDIR /home/gradle/app

# Copy everything (includes gradlew, build.gradle, src, etc.)
COPY --chown=gradle:gradle . .

# Build bootJar (Spring Boot fat jar)
RUN gradle clean bootJar -x test --no-daemon

# -------- Run stage --------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar
COPY --from=build /home/gradle/app/build/libs/*.jar app.jar

# Render sets PORT automatically; your app already uses ${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
