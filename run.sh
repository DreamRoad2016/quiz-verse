#!/usr/bin/env bash
# Always run with JDK 17+ (Spring Boot 3). Ignores JAVA_HOME if it is older.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"

java_major() {
  local home="$1"
  [[ -x "${home}/bin/java" ]] || return 1
  "${home}/bin/java" -version 2>&1 | awk -F[\".] '/version/ {print ($2=="1"?$3:$2); exit}'
}

pick_jdk17() {
  local candidates=(
    "${HOME}/.jdks/amazon-corretto-17.jdk/Contents/Home"
    "/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
    "${HOME}/Library/Java/JavaVirtualMachines/amazon-corretto-17.jdk/Contents/Home"
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ "$(java_major "$c" 2>/dev/null || true)" -ge 17 ]]; then
      echo "$c"
      return 0
    fi
  done
  if /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
    /usr/libexec/java_home -v 17
    return 0
  fi
  return 1
}

CURRENT_MAJOR="$(java_major "${JAVA_HOME:-}" 2>/dev/null || echo 0)"
if [[ "${CURRENT_MAJOR}" -lt 17 ]]; then
  if ! JAVA_HOME="$(pick_jdk17)"; then
    echo "需要 JDK 17+。当前 JAVA_HOME=${JAVA_HOME:-未设置}（major=${CURRENT_MAJOR}）。" >&2
    echo "请安装 Corretto/Temurin 17，或: export JAVA_HOME=\$(/usr/libexec/java_home -v 17)" >&2
    exit 1
  fi
  export JAVA_HOME
fi

export PATH="${JAVA_HOME}/bin:${PATH}"
echo "Using JAVA_HOME=${JAVA_HOME}"
"${JAVA_HOME}/bin/java" -version
cd "${ROOT}"
exec mvn spring-boot:run "$@"
