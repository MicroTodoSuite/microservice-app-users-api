FROM maven:3.9.12-eclipse-temurin-21@sha256:c3c9d3ac4ce8431a3995c0318b8d390f448e693dd4fabc16e9b68d2e1f3d7b46 AS build

WORKDIR /src
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean verify

FROM gcr.io/distroless/java21-debian13:nonroot@sha256:4e664bc71c4459c50407bbbeda96058f82c6b6d07d155d1ac39b8deca3cd42c0

WORKDIR /app
COPY --from=build --chown=nonroot:nonroot /src/target/users-api-0.0.1-SNAPSHOT.jar /app/app.jar

USER nonroot:nonroot
EXPOSE 8083
ENTRYPOINT ["/usr/bin/java", "-jar", "/app/app.jar"]
