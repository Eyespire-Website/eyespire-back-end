
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY . .
RUN mvn clean package -DskipTests


# Run stage

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/eyespire-api-0.0.1-SNAPSHOT.war eyespire-api.war
EXPOSE 8080

ENTRYPOINT ["java","-jar","eyespire-api.war"]
