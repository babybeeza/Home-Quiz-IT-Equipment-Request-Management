# syntax=docker/dockerfile:1
# Backend image for the Docker app/e2e profiles in compose.yaml (TASK-009). Tests are run separately (they need Docker for Testcontainers).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
# Windows checkouts may carry CRLF; the wrapper must be LF to run in Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -DskipTests package \
    && cp target/equipment-request-service-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=5s --timeout=3s --retries=30 CMD curl -fs http://localhost:8080/actuator/health/liveness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
