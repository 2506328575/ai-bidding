# AI 招投标系统 — 研发 SOP 总纲

基于可行性研究报告 v1.3（FastAPI-Amis-Admin 技术栈），拆解全岗位研发任务。

## 项目信息

| 项 | 值 |
|----|-----|
| 项目名称 | FA 销售全链路 AI 赋能项目 — 第一阶段：售前初步闭环 |
| 技术栈 | FastAPI(Python) + Vue3 + Amis Admin + Milvus + MySQL |
| 目标上线 | 2026 Q4 |

## 岗位与职责

| 岗位 | 核心交付物 | 输出目录 |
|------|-----------|---------|
| TeamLead | SOP总纲、任务清单、进度管控 | sop/ |
| 后端工程师 | FastAPI 后端代码 | backend/ |
| 前端工程师 | Vue3 + Amis 前端代码 | frontend/ |
| 数据库工程师 | SQL Schema + 种子数据 | database/ |
| 测试工程师 | 测试计划 + 用例 + Eval数据 | tests/ |
| DevOps工程师 | Docker Compose + CI/CD + 部署文档 | deploy/ |

## 模块清单（5大模块）

| 编号 | 模块 | 核心API |
|------|------|---------|
| M1 | 技术问答 | POST /api/v1/chat, POST /api/v1/qa/ask |
| M2 | 需求梳理 | (Phase 2 batch) |
| M3 | 方案生成 | POST /api/v1/proposal/generate |
| M4 | 标书撰写 | POST /api/v1/bid/fill, GET /api/v1/bid/templates |
| M5 | 政策打通 | 管理后台 CRUD |

## 里程碑

| 阶段 | 交付物 | 状态 |
|------|--------|------|
| 可研报告 | 可行性研究报告 v1.3 | ✅ 完成 |
| SOP拆解 | 各岗位SOP手册 | ← 当前 |
| 并行研发 | 6岗位并行产出代码 | ⏳ |
| 测试验收 | 测试报告 | ⏳ |
| 部署上线 | 部署包 + 文档 | ⏳ |
| 交付汇总 | 完整交付包 | ⏳ |
