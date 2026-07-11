# Multi-stage build for GitHub Access Report API
# Stage 1: build the Spring Boot jar
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

COPY pom.xml mvnw ./
COPY .mvn .mvn
COPY src src

RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests package \
    && cp target/*.jar /workspace/app.jar

# Stage 2: slim runtime image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget \
    && addgroup -S appgroup \
    && adduser -S appuser -G appgroup

USER appuser

COPY --from=build /workspace/app.jar /app/app.jar

EXPOSE 8080

# Actuator health used by orchestrators / docker compose
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
