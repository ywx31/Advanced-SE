# MathMate Weekly Agent

该服务只负责把 Spring Boot 生成的确定性 `WeeklyFacts` 转成中文周报文案，不负责计算成绩或决定推荐题量。

## 启动

```bash
cd assignments/assignment-03/agent-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```

## 配置 DeepSeek

先运行 `cp .env.example .env`，再把 `.env` 中的 `DEEPSEEK_API_KEY` 修改为：

```env
DEEPSEEK_API_KEY=填写自己密钥
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat
```

未填写 Key、网络失败、模型返回非法 JSON 或内容审核不通过时，服务会自动返回确定性模板，Spring Boot 也会再做一次校验和回退。

## API

- `GET /health`
- `POST /v1/weekly-narrative`
