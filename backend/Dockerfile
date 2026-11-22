# syntax=docker/dockerfile:1

# ---- Build stage: compile and package the Spring Boot jar ----
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

# Copy only what dependency resolution needs first. As long as pom.xml does not change, Docker
# reuses this layer and the dependencies are not downloaded again on every code change.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src src
# Tests are not run here: CI runs the full suite (they need Docker for Testcontainers, which is
# not available inside an image build).
RUN ./mvnw -B -ntp package -DskipTests \
    # Split the fat jar into layers ordered by how often they change: dependencies rarely,
    # application classes on every commit. Each becomes its own image layer below.
    && java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination target/extracted

# ---- Runtime stage: only a JRE and the application ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Never run the application as root inside the container.
RUN addgroup -S taskflow && adduser -S taskflow -G taskflow
USER taskflow

COPY --from=build /workspace/target/extracted/dependencies/ ./
COPY --from=build /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/target/extracted/application/ ./

ENV PORT=8080
EXPOSE 8080

# Docker (and docker compose "service_healthy") use this to know when the API is ready.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=5 \
    CMD wget -qO- "http://localhost:${PORT}/actuator/health" | grep -q '"UP"' || exit 1

# Respect the container's memory limit instead of the host's.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "org.springframework.boot.loader.launch.JarLauncher"]
