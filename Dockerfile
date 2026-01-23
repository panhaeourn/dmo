# ---------- Build stage ----------
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# Copy all project files
COPY . .

# Fix gradlew permission
RUN chmod +x ./gradlew

# Build Spring Boot jar (skip tests)
RUN ./gradlew clean bootJar -x test --no-daemon

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy built jar
COPY --from=build /app/build/libs/*.jar app.jar

# Render uses PORT env variable
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
