# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy only the pom.xml first to leverage Docker cache for dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add non-root user for security
RUN addgroup -S appuser && adduser -S appuser -G appuser

# Copy the JAR from the build stage to the production image
COPY --from=build --chown=appuser:appuser /app/target/*.jar app.jar
#RUN chown appuser:appuser app.jar (using COPY and chown in one command to reduce the layers and container size)

# Add metadata labels
LABEL maintainer="mohamed.medhat2199@gmail.com"
LABEL description="Spring Boot Loans Microservice"

# Expose the port your Spring Boot app runs on
EXPOSE 8090

# Switch to non-root user
USER appuser

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]