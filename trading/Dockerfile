# Build stage
FROM eclipse-temurin:17-jre AS runtime

# Copy the JAR file from the context directly into the runtime image.
COPY web/build/libs/web-0.0.1-SNAPSHOT.jar trading.jar

# Command to run the application
ENTRYPOINT ["java", "-jar", "trading.jar"]
