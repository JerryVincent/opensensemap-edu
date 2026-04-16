# ================================================================
# Stage 1: Build the application with Maven
# ================================================================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# ================================================================
# Stage 2: Run the application
# ================================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /build/target/*.jar app.jar

# Railway provides the PORT env variable dynamically
EXPOSE 8080

# Run the app - Spring Boot will read PORT from env
ENTRYPOINT ["java", "-jar", "app.jar"]