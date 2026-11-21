# =========================
# Stage 1: Build application
# =========================
FROM eclipse-temurin:17-jdk AS build

WORKDIR /workspace

# Copy Maven Wrapper and project configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Make Maven Wrapper executable
RUN chmod +x mvnw

# Download dependencies first to improve Docker layer caching
RUN ./mvnw -B dependency:go-offline

# Copy source code
COPY src/ src/

# Build Spring Boot executable JAR
RUN ./mvnw -B -DskipTests clean package

# =========================
# Stage 2: Runtime
# =========================
FROM eclipse-temurin:17-jre

WORKDIR /app

# Run application with a non-root user
RUN mkdir -p /app && chown 10001:10001 /app

# Copy executable JAR from build stage
COPY --from=build --chown=10001:10001 \
    /workspace/target/Backend-0.0.1-SNAPSHOT.jar \
    /app/app.jar

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]