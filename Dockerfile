# ---- Build stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /app

# Copy everything
COPY . .

# Build the jar (skip tests to speed up deploy)
RUN gradle clean build -x test --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copy the built jar into the runtime image
COPY --from=build /app/build/libs/*.jar app.jar

# Render provides $PORT, Spring uses server.port=${PORT:8080}
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
