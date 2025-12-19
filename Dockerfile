# Use a Java 17 image (or your chosen version)
FROM eclipse-temurin:21-jdk

# Set work directory
WORKDIR /app

# Copy Maven build output
COPY target/wallet-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the app
ENTRYPOINT ["java","-jar","app.jar"]
