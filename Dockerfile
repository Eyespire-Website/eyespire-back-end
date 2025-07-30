
FROM maven:3-openjdk-21 AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests


# Run stage

FROM openjdk:21-jdk-slim
WORKDIR /app

COPY --from=build /app/target/eyespire-api-0.0.1-SNAPSHOT.war eyespire-api.war
EXPOSE 8080

ENTRYPOINT ["java","-jar","eyespire-api.war"]
