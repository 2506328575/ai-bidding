# AI 招投标系统

FA 销售全链路 AI 赋能项目 — 第一阶段售前初步闭环。

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Python 3.10+ / FastAPI |
| ORM | SQLModel + SQLAlchemy |
| AI | DeepSeek API (httpx 异步调用) |
| 向量库 | Milvus (pymilvus) |
| 文档 | python-docx + lxml |

## 快速启动

```bash
# 安装依赖
cd backend && pip install -r requirements.txt

# 设置 API Key
export DEEPSEEK_API_KEY="sk-your-key"

# 启动 (端口 8080)
python main.py

# 验证
curl http://localhost:8080/health
curl -X POST http://localhost:8080/api/v1/chat -H 'Content-Type: application/json' -d '{"message":"hello"}'
```

## API 端点

| 方法 | 路径 | 模块 |
|------|------|------|
| POST | /api/v1/chat | 统一对话入口 |
| POST | /api/v1/qa/ask | 技术问答 |
| POST | /api/v1/proposal/generate | 方案生成 |
| POST | /api/v1/bid/fill | 标书填充 |
| POST | /api/v1/bid/upload | 模板上传 |
| GET | /api/v1/bid/templates | 模板列表 |
| POST | /api/v1/policy/create | 政策录入 |
| GET | /api/v1/policy/list | 政策查询 |
| GET | /api/v1/requirement/dimensions | 需求维度 |
| POST | /api/v1/requirement/session/start | 需求梳理会话 |

## 项目结构

```
├── backend/              # FastAPI 后端
│   ├── main.py
│   ├── core/settings.py
│   └── apps/
│       ├── qa/           # M1 技术问答
│       ├── proposal/     # M3 方案生成
│       ├── bid/          # M4 标书撰写
│       ├── policy/       # M5 政策打通
│       └── requirement/  # M2 需求梳理
├── database/             # SQL Schema + 种子数据
├── deploy/               # Docker + CI/CD
├── sop/                  # 7份研发SOP手册
├── tests/                # 测试计划
└── 可行性研究报告_*.md    # 可行性研究 v1.3
```

## 文档

- [可行性研究报告](可行性研究报告_FA销售全链路AI赋能项目_第一阶段.md)
- [SOP总纲](sop/00-研发SOP总纲.md)
