# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create directory for uploads
RUN mkdir -p /app/uploads
VOLUME /app/uploads

# Copy the WAR file from build stage
COPY --from=build /app/target/eyespire-api-*.war app.war

# Expose port 8080
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "app.war"]
