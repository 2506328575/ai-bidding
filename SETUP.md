# AI 招投标系统 — 环境搭建指南

## 前置依赖

| 工具 | 版本 | 安装 |
|------|------|------|
| Python | 3.10+ | `python.org` 或 `winget install python` |
| Git | 任意 | `git` |

## 首次克隆

```bash
git clone https://github.com/2506328575/ai-bidding.git
cd ai-bidding
cd backend && pip install -r requirements.txt
```

## 启动

```bash
export DEEPSEEK_API_KEY="sk-your-key"
cd backend && python main.py
# 服务运行在 http://localhost:8080
```

## API 文档

启动后访问:
- Swagger UI: http://localhost:8080/docs
- ReDoc: http://localhost:8080/redoc

## Docker 部署

```bash
cp deploy/.env.example .env
# 编辑 .env 填入 API Key
docker-compose -f deploy/docker-compose.yml up -d
```

## 多机器同步

```bash
# 下班前
git add -A && git commit -m "描述改动" && git push

# 上班后
git pull
```
