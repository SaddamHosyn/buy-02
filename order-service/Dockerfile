# =====================================================
# Dockerfile for Order Service
# =====================================================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY order-service/pom.xml order-service/

# Download dependencies (cached layer)
RUN chmod +x mvnw && \
   ./mvnw dependency:go-offline -pl order-service -am -B

# Copy source code
COPY order-service/src order-service/src

# Build the application
RUN ./mvnw package -pl order-service -am -DskipTests -B

# =====================================================
# Runtime Stage
# =====================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built JAR
COPY --from=builder /app/order-service/target/*.jar app.jar

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
   CMD wget -q --spider http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
