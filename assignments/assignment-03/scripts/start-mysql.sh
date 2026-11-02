#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MYSQL_BIN="${MYSQL_BIN:-}"
if [[ -z "$MYSQL_BIN" ]]; then
  if command -v mysqld >/dev/null 2>&1; then
    MYSQL_BIN="$(command -v mysqld)"
  elif [[ -x /usr/local/mysql/bin/mysqld ]]; then
    MYSQL_BIN="/usr/local/mysql/bin/mysqld"
  elif [[ -x /opt/homebrew/opt/mysql/bin/mysqld ]]; then
    MYSQL_BIN="/opt/homebrew/opt/mysql/bin/mysqld"
  else
    echo "未找到 mysqld。请安装 MySQL 8+，或设置 MYSQL_BIN。"
    exit 1
  fi
fi
MYSQL_HOME="$(cd "$(dirname "$MYSQL_BIN")/.." && pwd)"
MYSQLADMIN="$MYSQL_HOME/bin/mysqladmin"
SOCKET="${MYSQL_SOCKET:-/tmp/mathmate-mysql.sock}"
PORT="${MYSQL_PORT:-3306}"
ROOT_USER="${MYSQL_ROOT_USER:-root}"
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
ROOT_ARGS=(-u"$ROOT_USER")
if [[ -n "$ROOT_PASSWORD" ]]; then
  ROOT_ARGS+=("--password=$ROOT_PASSWORD")
fi

if "$MYSQLADMIN" --socket="$SOCKET" "${ROOT_ARGS[@]}" status >/dev/null 2>&1; then
  echo "MathMate MySQL 已运行（socket: $SOCKET）。"
  exit 0
fi
if "$MYSQLADMIN" -h127.0.0.1 -P"$PORT" "${ROOT_ARGS[@]}" status >/dev/null 2>&1; then
  echo "端口 $PORT 上已有 MySQL 服务，直接复用。"
  exit 0
fi
if nc -z -w 2 127.0.0.1 "$PORT" >/dev/null 2>&1; then
  echo "端口 $PORT 已被其他 MySQL/服务占用，但当前账号无法连接。"
  echo "请设置 MYSQL_ROOT_USER/MYSQL_ROOT_PASSWORD，或改用其他端口："
  echo "  export MYSQL_PORT=3307"
  echo "  export MYSQL_SOCKET=/tmp/mathmate-mysql-3307.sock"
  exit 1
fi

DATA_DIR="$PROJECT_ROOT/.mysql-data6"
if [[ ! -d "$DATA_DIR/mysql" ]]; then
  DATA_DIR="$PROJECT_ROOT/.mysql-data5"
fi
if [[ ! -d "$DATA_DIR/mysql" ]]; then
  DATA_DIR="$PROJECT_ROOT/.mysql-data4"
fi
if [[ ! -d "$DATA_DIR/mysql" ]]; then
  DATA_DIR="$PROJECT_ROOT/.mysql-data3"
fi
if [[ ! -d "$DATA_DIR/mysql" ]]; then
  DATA_DIR="$PROJECT_ROOT/.mysql-data2"
fi
if [[ ! -d "$DATA_DIR/mysql" ]]; then
  DATA_DIR="$PROJECT_ROOT/.mysql-data"
fi
RUN_DIR="$PROJECT_ROOT/.mysql-run-local"
mkdir -p "$DATA_DIR" "$RUN_DIR"

if [[ ! -d "$DATA_DIR/mysql" ]]; then
  echo "首次初始化项目 MySQL 数据目录：$DATA_DIR"
  "$MYSQL_BIN" --no-defaults --lower-case-table-names=2 \
    --initialize-insecure --basedir="$MYSQL_HOME" --datadir="$DATA_DIR" \
    --log-error="$RUN_DIR/init.err"
fi

echo "启动 MathMate MySQL（此终端请保持运行）..."
echo "socket: $SOCKET  port: $PORT"

MYSQL_PID=""
for attempt in 1 2 3 4 5; do
  "$MYSQL_BIN" --no-defaults --lower-case-table-names=2 \
    --basedir="$MYSQL_HOME" --datadir="$DATA_DIR" \
    --socket="$SOCKET" --port="$PORT" --bind-address=127.0.0.1 \
    --pid-file="$RUN_DIR/mysqld.pid" --log-error="$RUN_DIR/mysqld.err" &
  MYSQL_PID=$!
  for _ in $(seq 1 8); do
    if "$MYSQLADMIN" --socket="$SOCKET" "${ROOT_ARGS[@]}" status >/dev/null 2>&1; then
      echo "MathMate MySQL 启动成功。"
      wait "$MYSQL_PID"
      exit $?
    fi
    sleep 1
  done
  if kill -0 "$MYSQL_PID" 2>/dev/null; then
    kill "$MYSQL_PID" 2>/dev/null || true
    wait "$MYSQL_PID" 2>/dev/null || true
  fi
  echo "第 $attempt 次启动未成功，正在重试..."
  sleep 1
done

echo "MySQL 启动失败，请查看：$RUN_DIR/mysqld.err"
exit 1
