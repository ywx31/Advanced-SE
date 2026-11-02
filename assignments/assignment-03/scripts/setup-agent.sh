#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT_ROOT/agent-service"

if [[ ! -x ".venv/bin/python" ]]; then
  python3 -m venv .venv
fi

# Some macOS Python installations need explicit CA trust for pip downloads.
.venv/bin/pip install \
  --trusted-host pypi.org \
  --trusted-host files.pythonhosted.org \
  -r requirements.txt

if [[ ! -f ".env" ]]; then
  cp .env.example .env
fi

echo "Agent 依赖已安装。请编辑 agent-service/.env 填写 DEEPSEEK_API_KEY。"
