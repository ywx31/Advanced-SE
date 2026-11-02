#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MVN_BIN="${MVN_BIN:-}"
if [[ -z "$MVN_BIN" ]]; then
  if command -v mvn >/dev/null 2>&1; then
    MVN_BIN="$(command -v mvn)"
  elif [[ -x "/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn" ]]; then
    MVN_BIN="/Applications/IntelliJ IDEA.app/Contents/plugins/maven-plugin/lib/maven3/bin/mvn"
  else
    echo "未找到 Maven。请安装 Maven，或设置 MVN_BIN。"
    exit 1
  fi
fi
if [[ "${MYSQL_PORT:-3306}" != "3306" && -z "${DB_URL:-}" ]]; then
  export DB_URL="jdbc:mysql://127.0.0.1:${MYSQL_PORT}/mathmate?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
fi

if [[ -f "$PROJECT_ROOT/scripts/oss.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$PROJECT_ROOT/scripts/oss.env"
  set +a
fi

if [[ -z "${JAVA_HOME:-}" ]] && /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
  JAVA_HOME="$(/usr/libexec/java_home -v 17)"
  export JAVA_HOME
fi

BACKEND_PID=""
FRONTEND_PID=""
AGENT_PID=""

cleanup() {
  [[ -n "$AGENT_PID" ]] && kill "$AGENT_PID" 2>/dev/null || true
  [[ -n "$BACKEND_PID" ]] && kill "$BACKEND_PID" 2>/dev/null || true
  [[ -n "$FRONTEND_PID" ]] && kill "$FRONTEND_PID" 2>/dev/null || true
}
trap cleanup EXIT INT TERM

if [[ -x "$PROJECT_ROOT/agent-service/.venv/bin/uvicorn" ]]; then
  (cd "$PROJECT_ROOT/agent-service" && .venv/bin/uvicorn app.main:app --host 127.0.0.1 --port 8000) &
  AGENT_PID=$!
  echo "Agent:   http://127.0.0.1:8000"
else
  echo "Agent:   未安装 Python 依赖，将使用 Java 模板回退（可运行 scripts/setup-agent.sh）"
fi

(cd "$PROJECT_ROOT/backend" && "$MVN_BIN" spring-boot:run) &
BACKEND_PID=$!
(cd "$PROJECT_ROOT/frontend" && npm run dev) &
FRONTEND_PID=$!

echo "Backend: http://localhost:8080"
echo "Frontend: http://localhost:5173"
wait
