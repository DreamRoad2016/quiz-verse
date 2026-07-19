#!/usr/bin/env bash
# Run with JDK 17: uses JAVA_HOME if set, else ~/.jdks Corretto 17, else java_home -v 17
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  if [[ -x "${HOME}/.jdks/amazon-corretto-17.jdk/Contents/Home/bin/java" ]]; then
    export JAVA_HOME="${HOME}/.jdks/amazon-corretto-17.jdk/Contents/Home"
  elif /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  else
    echo "需要 JDK 17。请设置 JAVA_HOME，或安装 Corretto/Temurin 17。" >&2
    exit 1
  fi
fi
export PATH="${JAVA_HOME}/bin:${PATH}"
echo "Using JAVA_HOME=${JAVA_HOME}"
"${JAVA_HOME}/bin/java" -version
cd "${ROOT}"
exec mvn spring-boot:run "$@"
