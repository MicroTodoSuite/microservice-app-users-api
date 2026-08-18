#!/usr/bin/env bash
# Unit gate entrypoint (spec 006 / T012) consumed by the reusable CI `unit` job.
# Runs the JUnit suite via Maven; the jacoco-maven-plugin (bound to the test
# phase) writes target/site/jacoco/jacoco.xml for the SonarQube quality gate,
# which enforces the 70% threshold over the business classes (bootstrap/config
# excluded, research D2).
set -euo pipefail
mvn -B test
