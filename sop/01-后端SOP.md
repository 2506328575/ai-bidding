# 后端工程师 SOP

## 技术栈
- FastAPI 0.100+ (Python 3.10+)
- SQLModel + SQLAlchemy (ORM)
- httpx (LLM API 异步调用)
- pymilvus (向量检索)
- python-docx + lxml (Word 生成)
- APScheduler (定时任务)

## 目录结构
```
backend/
├── main.py                 # FastAPI 入口
├── core/settings.py        # 全局配置
├── core/admin.py           # Amis Admin 注册
├── apps/
│   ├── qa/                 # M1 技术问答
│   │   ├── apis.py
│   │   ├── models.py
│   │   ├── schemas.py
│   │   └── services/
│   │       ├── intent.py   # 意图分类
│   │       └── rag.py      # RAG 检索
│   ├── proposal/           # M3 方案生成
│   │   ├── apis.py
│   │   └── services/
│   │       ├── outline.py
│   │       └── docx_render.py
│   ├── bid/                # M4 标书撰写
│   │   ├── apis.py
│   │   └── services/
│   │       ├── template.py
│   │       └── filler.py
│   ├── policy/             # M5 政策打通
│   │   └── apis.py
│   └── requirement/        # M2 需求梳理
│       └── apis.py
├── prompts/                # Prompt YAML
│   ├── intent.yaml
│   ├── qa.yaml
│   ├── proposal.yaml
│   └── bid.yaml
└── templates/              # Word 模板
```

## API 端点清单

| 方法 | 路径 | 模块 | 说明 |
|------|------|------|------|
| POST | /api/v1/chat | M1 | 统一对话入口 |
| POST | /api/v1/qa/ask | M1 | 技术问答 |
| POST | /api/v1/proposal/generate | M3 | 方案生成 |
| POST | /api/v1/bid/fill | M4 | 标书填充 |
| GET | /api/v1/bid/templates | M4 | 模板列表 |
| POST | /api/v1/bid/download | M4 | 下载标书 |
| POST | /api/v1/template/upload | 模板 | 上传模板 |
| GET | /api/v1/template/list | 模板 | 模板列表 |
| POST | /api/v1/template/{id}/fill | 模板 | AI填充 |

## 开发顺序
1. core/settings.py + main.py (1天)
2. M1 技术问答 (2天)
3. M3 方案生成 (2天)
4. M4 标书撰写 (2天)
5. M5 政策打通 + M2 需求梳理 (2天)
6. 集成测试 (1天)

## 参照
- Java Demo: yudao-module-ai-bidding/ (功能验证通过)
- 可行性研究报告: 第2节(技术架构) + 第3节(模块分析)
