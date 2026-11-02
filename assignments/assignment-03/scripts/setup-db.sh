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

SOCKET="/tmp/mathmate-mysql.sock"
if [[ -S "$SOCKET" ]]; then
  MYSQL_ARGS=(--socket="$SOCKET" -uroot)
else
  MYSQL_ARGS=(-h127.0.0.1 -P3306 -uroot)
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
REPORT_TABLE_EXISTS=$("$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" -N -B -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='mathmate' AND table_name='weekly_reports'")
if [[ "$REPORT_TABLE_EXISTS" == "0" ]]; then
  "$MYSQL_CLIENT" "${MYSQL_ARGS[@]}" mathmate < "$PROJECT_ROOT/db/migration_v4_weekly_reports.sql"
  echo "已升级学习周报与 Agent 调用记录表。"
fi

echo "MySQL 数据库 mathmate 和用户 mathmate 已创建。"
