## Overview
The Users API serves JWT-protected user lookup and listing endpoints, plus an in-process request counter.
It stores seeded users through Spring Data JPA and exposes Prometheus metrics while sending traces to Zipkin.

## Stack
- Java 8 (`java.version` 1.8), packaged as a Maven JAR; Maven Wrapper 3.5.0 is checked in.
- Spring Boot 1.5.6.RELEASE with Spring MVC, Security, and Data JPA.
- H2 1.4.197, JJWT 0.7.0, Spring Cloud Zipkin 1.3.1.RELEASE, and Prometheus clients 0.2.0.
- Tests use Spring Boot Test and JUnit 4.13.2.
- Node.js 22 and semantic-release 24.2.3 are CI release tooling only, not application runtime dependencies.

## Commands
- Build (README): `./mvnw clean install`
- Test and package (Dockerfile; Maven's package lifecycle runs tests): `mvn clean package`
- Local run (README): `JWT_SECRET=PRFT SERVER_PORT=8083 java -jar target/users-api-0.0.1-SNAPSHOT.jar`
- No standalone Java test command is documented. The `npm test` script is a placeholder that exits 1, and the sole Java test only loads the Spring context.
- The tracked Unix `mvnw` mode is `100644`, so the documented build command currently fails with `Permission denied` until its executable bit is corrected.

## Structure
- `src/main/java/com/elgris/usersapi/api/`: `/users/`, `/users/{username}`, and `/count` REST controllers.
- `src/main/java/com/elgris/usersapi/security/`: JWT parsing filter and the unused `AccessUserFilter`.
- `src/main/java/com/elgris/usersapi/configuration/`: Spring Security filter-chain configuration.
- `src/main/java/com/elgris/usersapi/models/` and `repository/`: JPA user model, roles, and Spring Data repository.
- `src/main/resources/`: environment-backed application properties, H2 seed data, and Prometheus Logback setup.
- `src/test/java/`: the single `@SpringBootTest` context-load test.
- `.github/workflows/`: semantic-release automation and the current Azure Container Apps deployment workflow.
- `package.json`, `package-lock.json`, and `.releaserc`: release automation only; application dependencies are in `pom.xml`.

## Conventions
- `JwtAuthenticationFilter` applies to all routes except `/metrics`, `/prometheus`, and `/actuator*`; `OPTIONS` is also passed through.
- `/users/{username}` accepts only the username in the JWT `username` claim; `/users/` returns all seeded users.
- `data.sql` seeds three H2 users at startup; there is no service layer between controllers and `UserRepository`.
- `/count` and its count are process-local; Prometheus JVM, HTTP timing, counter, and Logback metrics are enabled in code.

## Notes for the Kubernetes migration
- The application listens on `SERVER_PORT`, default `8083`; the Dockerfile declares neither `EXPOSE` nor `HEALTHCHECK`.
- Supported environment variables and defaults are `JWT_SECRET=myfancysecret`, `SERVER_PORT=8083`, `SPRING_APPLICATION_NAME=users-api`, `ZIPKIN_URL=http://zipkin:9411/`, and `SLEUTH_SAMPLER_PROBABILITY=1.0`.
- Supply `JWT_SECRET` from a Kubernetes Secret and keep it aligned with JWT-issuing components; do not use the checked-in fallback in an environment.
- Zipkin is the only configured external network dependency. No Redis or external database is configured; H2 and the seeded users are pod-local and must be reviewed for multi-replica operation and restarts.
- The README identifies Auth API as the JWT issuer, but this service makes no HTTP call to it. The `/count` value is also not shared across replicas.
- Review probe paths and the `management.endpoints.*` properties against Spring Boot 1.5.6; the JWT filter only proves that observability paths bypass authentication.
- Review the Java 8 base images, root container user, broad `COPY . .`, wildcard JAR copy, missing `.dockerignore`, and absent container health check.
- The Azure workflow builds with `maven:3.8.4-openjdk-8`, runs on `openjdk:8-jre-slim`, pushes release and `latest` tags to ACR, then directly updates and restarts an Azure Container App.
- No Container Apps manifest defines ingress, environment, secrets, scaling, or probes in this repo; inventory that external configuration before migration.
- Kubernetes delivery must update `microservice-app-gitops` and let ArgoCD reconcile it; never use direct `kubectl apply` for a managed environment.
