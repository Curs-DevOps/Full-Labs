######## Stage 1: Build stage
FROM amazoncorretto:17-alpine3.22-jdk as builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first (for layer caching)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Download dependencies (cached if pom.xml hasn't changed)
RUN ./mvnw dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

######## Stage 2: Runtime stage
FROM amazoncorretto:17-alpine3.22-jdk as runner

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring

# Set working directory
WORKDIR /app

ARG INCLUDE_TLS=false

# Copy certificate and create truststore
RUN if [ "$INCLUDE_TLS" = "true" ]; then \
      echo "Copying and importing TLS cert..."; \
      if [ -d "tls" ]; then \
        cp -r tls /tmp/; \
        keytool -import -trustcacerts -noprompt \
          -alias vault \
          -file /tmp/tls/vault.crt \
          -keystore /app/truststore.jks \
          -storepass changeit; \
        chown spring:spring /app/truststore.jks; \
        rm /tmp/tls/vault.crt; \
      else \
        echo "TLS directory not found, skipping cert import."; \
      fi; \
    else \
      echo "Skipping truststore build."; \
    fi

# Copy the jar from builder stage
COPY --from=builder /build/target/*.jar app.jar

# === START: Custom Entrypoint Integration ===

# Copy the startup script
COPY scripts/start.sh /usr/local/bin/start.sh

# Make the script executable
RUN chmod +x /usr/local/bin/start.sh

# === END: Custom Entrypoint Integration ===

# Change ownership to non-root user
RUN chown -R spring:spring /app /usr/local/bin/start.sh

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application by calling our custom script
# This ENTRYPOINT will now handle the wait-for-vault logic.
ENTRYPOINT ["/usr/local/bin/start.sh"]
