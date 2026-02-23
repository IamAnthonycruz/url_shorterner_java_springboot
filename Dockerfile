# Use lightweight Java runtime
FROM eclipse-temurin:21-jdk-alpine

# Set working directory inside container
WORKDIR /app

# Copy built jar into container
COPY target/*.jar app.jar

# Expose Spring port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]