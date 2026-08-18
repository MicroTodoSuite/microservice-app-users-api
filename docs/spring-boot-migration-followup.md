# Follow-up: migrate users-api to Spring Boot 2/3

**Status**: tracked follow-up (owner decision 2026-08-17) — out of scope for spec
006 (testing-and-hardening).

## Why

users-api runs Spring Boot 1.5.6.RELEASE on Java 8. Spec 006 remediated every
vulnerability that is fixable *within that framework line* (see `pom.xml`:
logback, tomcat 8.5.51, jackson 2.8.11, spring 4.3.15, spring-data Ingalls-SR12,
hibernate-validator 5.3.6, snakeyaml 1.26, bcprov 1.56).

The remaining HIGH/CRITICAL findings (recorded as risk-accepted exceptions in
`.trivyignore`, review-by 2026-11-30) are **framework-locked**: each is only fixed
in a version that requires Spring Boot 2/3 — i.e. Java 11/17 and a real migration,
not a dependency bump. This is the "legacy stacks limit remediation" case called
out in the plan's Complexity Tracking.

## Scope of the migration

- Java 8 → 17; Spring Boot 1.5.6 → 2.7.x (bridge) → 3.x.
- Security: `WebSecurityConfigurerAdapter` → `SecurityFilterChain`; Spring
  Security 4 → 6 APIs.
- Persistence: `javax.persistence` → `jakarta.persistence`; Hibernate 5.0 → 6.
- Actuator/metrics endpoints and the Prometheus client wiring.
- Re-verify the unit suite (`ci/unit.sh`) and re-scan to drop the `.trivyignore`
  entries once resolved.

## Acceptance

- `trivy` reports zero fixable HIGH/CRITICAL with an empty `.trivyignore`.
- Existing unit tests (UsersController, JwtAuthenticationFilter) pass unchanged
  in behavior.
