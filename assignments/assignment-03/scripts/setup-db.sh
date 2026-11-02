#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MYSQL_CLIENT="${MYSQL_CLIENT:-}"
if [[ -z "$MYSQL_CLIENT" ]]; then
  if command -v mysql >/dev/null 2>&1; then
    MYSQL_CLIENT="$(command -v mysql)"
  elif [[ -x /usr/local/mysql/bin/mysql ]]; then
    MYSQL_CLIENT="/usr/local/mysql/bin/mysql"
  elif [[ -x /opt/homebrew/bin/mysql ]]; then
    MYSQL_CLIENT="/opt/homebrew/bin/mysql"
  else
    echo "未找到 mysql 客户端。请先安装 MySQL 8+。"
    exit 1
  fi
fi

SOCKET="${MYSQL_SOCKET:-/tmp/mathmate-mysql.sock}"
PORT="${MYSQL_PORT:-3306}"
ROOT_USER="${MYSQL_ROOT_USER:-root}"
ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
MYSQL_AUTH=(-u"$ROOT_USER")
if [[ -n "$ROOT_PASSWORD" ]]; then
  MYSQL_AUTH+=("--password=$ROOT_PASSWORD")
fi
if [[ -S "$SOCKET" ]]; then
  MYSQL_ARGS=(--socket="$SOCKET" "${MYSQL_AUTH[@]}")
else
  MYSQL_ARGS=(-h127.0.0.1 -P"$PORT" "${MYSQL_AUTH[@]}")
fi

"$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" < "$PROJECT_ROOT/db/init.sql"

TABLE_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mathmate' AND table_name='wrong_questions'")
if [[ "$TABLE_EXISTS" == "1" ]]; then
  COLUMN_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='mathmate' AND table_name='wrong_questions' AND column_name='question_type'")
  if [[ "$COLUMN_EXISTS" == "0" ]]; then
    "$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" mathmate < "$PROJECT_ROOT/db/migration_v2_wrong_question_type.sql"
    echo "已升级错题本题型字段。"
  fi
fi

USER_TABLE_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mathmate' AND table_name='users'")
if [[ "$USER_TABLE_EXISTS" == "1" ]]; then
  AVATAR_COLUMN_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
    "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='mathmate' AND table_name='users' AND column_name='avatar_url'")
  if [[ "$AVATAR_COLUMN_EXISTS" == "0" ]]; then
    "$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" mathmate < "$PROJECT_ROOT/db/migration_v3_avatar.sql"
    echo "已升级用户头像字段。"
  fi
fi
if [[ "$USER_TABLE_EXISTS" == "1" ]]; then
  REPORT_TABLE_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mathmate' AND table_name='weekly_reports'")
  if [[ "$REPORT_TABLE_EXISTS" == "0" ]]; then
    "$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" mathmate < "$PROJECT_ROOT/db/migration_v4_weekly_reports.sql"
    echo "已升级学习周报与 Agent 调用记录表。"
  fi
else
  echo "首次初始化：业务表将由 Spring Boot 启动时自动创建。"
fi

echo "MySQL 数据库 mathmate 和用户 mathmate 已创建。"
