# 数据库工程师 SOP

## 数据库选型
- MySQL 8.0 (生产) / SQLite (开发)
- Milvus 2.x (向量库，独立部署)

## Schema 文件

### schema.sql — 核心表

| 表名 | 说明 | 模块 |
|------|------|------|
| prompt_template | Prompt 模板表 | 全局 |
| requirement_dimension | 需求采集维度配置 | M2 |
| bid_format_rule | 标书格式校验规则 | M4 |
| policy_record | 政策记录 | M5 |
| proposal_record | 方案生成记录 | M3 |
| bid_record | 标书生成记录 | M4 |
| template_file | 模板文件元数据 | 模板管理 |
| chat_session | 对话会话 | M1 |
| chat_message | 对话消息 | M1 |
| llm_call_log | LLM 调用日志 | 全局 |

### 关键索引策略
- prompt_template: UNIQUE(key, version)
- policy_record: INDEX(province, city, category, effective_date)
- chat_message: INDEX(session_id, created_at)
- llm_call_log: INDEX(created_at, model, tokens_used)

### seed_data.sql — 初始数据
- 10 条 Prompt 模板 (intent.classify, qa.rag.answer, proposal.outline, proposal.section, bid.fill_section 等)
- 5 条需求采集维度 (行业/规模/痛点/预算/时限)
- 6 条标书格式校验规则 (字体/页边距/页码/目录/盖章/页数限制)

## 版本管理
- migrations/001_initial_schema.sql
- 后续变更按序号递增
- 每次迁移包含 UP 和 DOWN 脚本

## 开发顺序
1. schema.sql (0.5天)
2. seed_data.sql (0.5天)
3. migrations/ (同后端联调时增量)
