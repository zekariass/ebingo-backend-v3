# Stage 1: Build the application
FROM maven:3.9.4-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
COPY src ./src

# Build the Spring Boot app and enable layered JAR
RUN mvn clean package -DskipTests

# Stage 2: Run with optimized JDK
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# create non-root user
RUN addgroup -S app && adduser -S app -G app

COPY --from=builder /app/target/*.jar app.jar

# optional: set a default JAVA_OPTS if not passed
ENV JAVA_OPTS="-Xms512m -Xmx2g -Djava.security.egd=file:/dev/./urandom"

# healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD wget --spider -q http://localhost:8080/actuator/health || exit 1

USER app
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]

