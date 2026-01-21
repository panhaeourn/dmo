# ---- Build stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

COPY . .

# Use Gradle installed in the image (no need for ./gradlew permissions)
RUN gradle clean build -x test --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
