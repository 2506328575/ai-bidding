# 测试工程师 SOP

## 测试体系

| 类型 | 工具 | 覆盖目标 |
|------|------|---------|
| 后端单元测试 | pytest + httpx | Service 层 100% |
| 后端集成测试 | pytest + TestClient | API 端点 |
| AI 质量 Eval | 自定义 eval harness | 意图分类 / RAG / 内容质量 |
| 前端组件测试 | vitest + @vue/test-utils | 组件渲染 + 交互 |
| E2E 测试 | Playwright | 完整用户流程 |

## 测试用例分布

| 模块 | 单元 | 集成 | Eval | E2E |
|------|------|------|------|-----|
| M1 技术问答 | 8 | 3 | 6 | 2 |
| M2 需求梳理 | 4 | 2 | 2 | 1 |
| M3 方案生成 | 6 | 3 | 4 | 2 |
| M4 标书撰写 | 8 | 4 | 4 | 2 |
| M5 政策打通 | 4 | 2 | 0 | 1 |
| SQL 安全 | 4 | 0 | 0 | 0 |
| **合计** | **34** | **14** | **16** | **8** |

## AI Eval 数据集

| 数据集 | 条数 | 指标 |
|--------|------|------|
| intent-classify-eval.json | 20 | 准确率 ≥ 95% |
| rag-retrieval-eval.json | 15 | Top-3 召回率 ≥ 80% |
| qa-answer-eval.json | 10 | 相关性 ≥ 4/5 |
| proposal-quality-eval.json | 5 | 人工评分 ≥ 3/5 |

## 测试环境
- 后端: pytest + SQLite 内存库
- 前端: jsdom
- E2E: Playwright + Chromium
- Eval: 独立标注数据集 (JSON)

## Bug 闭环流程
1. 测试发现 → 记录 test_cases/bugs.md
2. 开发修复 → 更新对应测试用例
3. 回归测试 → 确认修复
4. 关闭 Bug → 标记 resolved
