#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

grep -Fq '<tomcat.version>10.1.59</tomcat.version>' "$repo_root/pom.xml" || {
  echo "runtime-security-contract: embedded Tomcat must include the 10.1.x security fixes" >&2
  exit 1
}

echo "runtime-security-contract: PASS"
