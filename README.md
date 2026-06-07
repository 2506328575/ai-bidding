# AI 招投标系统 Demo

FA 销售全链路 AI 赋能项目 —— 第一阶段售前初步闭环的可行性验证原型。

## 功能

| 功能 | 说明 |
|------|------|
| 💬 **对话助手** | 统一对话入口，AI 自动识别意图，检索案例/解答技术问题 |
| 📄 **方案生成** | 输入需求 → AI 逐节撰写售前方案 → 在线编辑 → 下载 Word/PPT |
| 📋 **标书填充** | 上传模板 → 提取/标记占位符 → AI 填充内容 → 在线编辑 → 下载 |
| 🗂️ **模板管理** | 上传 docx 模板，自动提取 `{{占位符}}`，支持手动添加 |

## 技术栈

| 层 | 技术 |
|----|------|
| 后端 | Java 17 + Spring Boot 3.2 + Apache POI + POI-TL |
| 前端 | Vue 3 + TypeScript + Vite + Element Plus |
| AI | DeepSeek API（兼容 OpenAI 格式） |
| 文档编辑 | OnlyOffice Web（ranuts/document） |

## 快速启动

**前置：** JDK 17+ / Node.js 20+ / Python 3.10+ / Maven

```bash
# 1. 安装文档编辑器
git clone https://github.com/ranuts/document.git document-editor
cd document-editor && npm install && cd ..

# 2. 安装前端依赖
cd ai-bidding-frontend && npm install && cd ..

# 3. 设置 API Key + 启动（需 3 个终端）
# 终端1: 后端
export DEEPSEEK_API_KEY="sk-your-key"
cd yudao-module-ai-bidding && ./mvnw spring-boot:run

# 终端2: 前端
cd ai-bidding-frontend && npm run dev

# 终端3: 文档编辑器
cd document-editor && npm run dev -- --port 3000
```

浏览器打开 **http://localhost:5173**

## 运行测试

```bash
# 后端 (63 条)
cd yudao-module-ai-bidding && ./mvnw test

# 前端 (15 条)
cd ai-bidding-frontend && npm test
```

## 项目结构

```
├── yudao-module-ai-bidding/    # Spring Boot 后端
│   └── src/main/java/.../aibidding/
│       ├── controller/         # REST API (Chat/Qa/Proposal/Bid/Template)
│       ├── service/llm/        # LLM/Prompt/RAG/Proposal/Bid 核心逻辑
│       └── config/             # Spring 配置
├── ai-bidding-frontend/        # Vue 3 前端
│   └── src/
│       ├── views/              # ChatView/BidFillView/ProposalView/TemplateManageView
│       ├── components/         # ChatMessage/ChatInput/IntentBadge/SourceCard
│       └── api/                # Axios API 封装
├── document-editor/            # OnlyOffice Web 文档编辑器 (git clone 获取)
├── Demo-SOP-AI开发流程.md       # 后端 AI 开发 SOP
├── Demo-SOP-前端-AI开发流程.md   # 前端开发 SOP
├── 可行性研究报告_*.md          # 可行性研究 v1.2
└── Ai招投标模板/                # 客户提供的原始模板文件
```

## 文档

- [可行性研究报告](可行性研究报告_FA销售全链路AI赋能项目_第一阶段.md)
- [环境搭建指南](SETUP.md)
