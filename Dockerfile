# Use official OpenJDK image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy built jar file to container
COPY target/*.jar app.jar

# Expose port (same as in your application.properties)
EXPOSE 8080

# Run jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
