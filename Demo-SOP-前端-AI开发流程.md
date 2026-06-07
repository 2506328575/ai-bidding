# Demo 前端 AI 开发 SOP 流程

## 文档说明

本 SOP 定义 AI 辅助开发前端 Demo 的标准流程。与后端 SOP 对应，每个 Phase 包含 **编写代码 → 编写测试 → 验证通过 → 进入下一 Phase**。

**Demo 目标：** 构建一个可与后端 QA API 交互的前端界面，验证前后端分离架构下的完整用户链路。

**用户交互流程：**

```
用户在输入框输入问题 → 点击发送
  → 前端调用 POST /api/v1/qa/ask
  → 展示：意图标签 + AI 回答 + 引用来源
  → 支持连续对话（多轮问答）
```

**技术栈：** Vue 3 + TypeScript + Vite + Element Plus + Axios + Vitest

**前后端关系：**

```
┌─────────────────┐     HTTP/REST      ┌─────────────────┐
│   Vue 3 前端     │ ◄──────────────► │   Spring Boot    │
│   localhost:5173 │    JSON           │   localhost:8080 │
│   (Vite dev)     │                   │   (Java)         │
└─────────────────┘                    └─────────────────┘
```

---

## Phase 0: 项目环境初始化

### 0.1 目标
用 Vite 创建 Vue 3 + TypeScript 项目，安装依赖，确认能启动。

### 0.2 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 0.2.1 | `npm create vite@latest ai-bidding-frontend -- --template vue-ts` | 项目创建成功 |
| 0.2.2 | `npm install` | 依赖安装无报错 |
| 0.2.3 | `npm install element-plus axios` | UI 库 + HTTP 客户端 |
| 0.2.4 | `npm install -D vitest @vue/test-utils jsdom` | 测试依赖 |
| 0.2.5 | 创建目录结构 | 见下方 |
| 0.2.6 | `npm run dev` → 打开浏览器看到 Vite 默认页 | 开发服务器启动 |

### 0.3 目录结构

```
ai-bidding-frontend/
├── src/
│   ├── api/                  # API 调用封装
│   │   └── qa.ts             # QA 接口
│   ├── components/           # 可复用组件
│   │   ├── ChatInput.vue     # 输入框组件
│   │   ├── ChatMessage.vue   # 消息气泡组件
│   │   ├── SourceCard.vue    # 引用来源卡片
│   │   └── IntentBadge.vue   # 意图标签
│   ├── views/                # 页面
│   │   └── QaView.vue        # 技术问答页
│   ├── types/                # TypeScript 类型
│   │   └── qa.ts             # 问答相关类型
│   ├── App.vue
│   ├── main.ts
│   └── style.css
├── src/__tests__/            # 测试文件
│   ├── components/
│   └── api/
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

### 0.4 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T0.1 | 单元 | App 组件渲染 | 页面包含"AI 售前助手"标题 |

---

## Phase 1: TypeScript 类型定义 + API 封装

### 1.1 目标
定义前后端数据契约（TypeScript 类型），封装 Axios HTTP 调用。

### 1.2 类型定义

```typescript
// src/types/qa.ts

/** 后端返回的引用来源 */
export interface Source {
  title: string
  relevance: string
}

/** QaController.QaResponse 的前端映射 */
export interface QaResponse {
  answer: string
  intent: string
  sources: Source[]
  tokensUsed: number
  latencyMs: number
}

/** 发送给后端的请求 */
export interface QaRequest {
  question: string
}

/** 对话中的一条消息 */
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  intent?: string
  sources?: Source[]
  tokensUsed?: number
  latencyMs?: number
  timestamp: number
}
```

### 1.3 API 封装

```typescript
// src/api/qa.ts

import axios from 'axios'
import type { QaRequest, QaResponse } from '../types/qa'

const apiClient = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

export async function askQuestion(request: QaRequest): Promise<QaResponse> {
  const { data } = await apiClient.post<QaResponse>('/qa/ask', request)
  return data
}
```

### 1.4 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 1.4.1 | 创建 `src/types/qa.ts` | 编译无错误 |
| 1.4.2 | 创建 `src/api/qa.ts` | — |
| 1.4.3 | 配置 Vite proxy → 后端 8080 | 开发时无需跨域处理 |

### 1.5 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T1.1 | 单元 | `askQuestion()` 正常返回 | 返回对象包含 answer/intent/sources 字段 |
| T1.2 | 单元 | `askQuestion()` 网络错误 | 抛出错误，不为静默失败 |
| T1.3 | 单元 | `askQuestion()` 后端返回 500 | 抛出错误，含状态码信息 |

```typescript
// src/__tests__/api/qa.test.ts
import { describe, it, expect, vi } from 'vitest'
import axios from 'axios'

vi.mock('axios')

describe('askQuestion', () => {
  it('T1.1: 正常返回 QaResponse', async () => {
    const mockData = {
      answer: '根据案例库...',
      intent: 'CASE_RETRIEVAL',
      sources: [{ title: 'XX制造案例', relevance: '0.9' }],
      tokensUsed: 150,
      latencyMs: 2300
    }
    vi.mocked(axios.create).mockReturnValue({
      post: vi.fn().mockResolvedValue({ data: mockData })
    } as any)
    // ...
  })

  it('T1.2: 网络错误时抛出异常', async () => {
    // mock axios.post 抛出 NetworkError
  })

  it('T1.3: 后端 500 时抛出异常含状态码', async () => {
    // mock axios.post 抛出 { response: { status: 500 } }
  })
})
```

---

## Phase 2: 核心组件开发

### 2.1 目标
构建三个核心 UI 组件，每个独立可测试。

### 2.2 组件清单

#### 2.2.1 IntentBadge — 意图标签

```
┌──────────────┐
│ 案例检索      │  ← 不同意图显示不同颜色
└──────────────┘

CASE_RETRIEVAL → 蓝色
PRODUCT_QUERY  → 绿色
POLICY_QUERY   → 橙色
DATA_QUERY     → 紫色
GREETING       → 灰色
```

**Props:** `intent: string`
**测试:** 传入不同 intent，验证颜色 class 正确

#### 2.2.2 SourceCard — 引用来源卡片

```
┌─────────────────────────────────┐
│ 📄 XX制造集团ERP实施案例        │
│    匹配度: 0.9                  │
└─────────────────────────────────┘
```

**Props:** `source: { title: string, relevance: string }`
**测试:** 验证 title 和 relevance 正确渲染

#### 2.2.3 ChatMessage — 消息气泡

```
用户消息（右侧，蓝色气泡）：
┌──────────────────────┐
│ 有没有制造业的案例？   │
└──────────────────────┘

AI 回复（左侧，白色气泡）：
┌─────────────────────────────────────┐
│ [案例检索]                          │
│                                     │
│ 根据案例库资料，我们曾为 XX 制造     │
│ 集团实施过 ERP 项目...              │
│                                     │
│ 📄 来源: XX制造集团ERP实施案例      │
│                                     │
│ ⏱ 2300ms  🎫 150 tokens            │
└─────────────────────────────────────┘
```

**Props:** `message: ChatMessage`
**测试:** 用户消息和 AI 消息渲染位置不同（右侧 vs 左侧）

### 2.3 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T2.1 | 组件 | IntentBadge — CASE_RETRIEVAL | 显示"案例检索"，颜色蓝色 |
| T2.2 | 组件 | IntentBadge — 未知 intent | 显示 intent 原文，默认灰色 |
| T2.3 | 组件 | SourceCard — 渲染 title + relevance | DOM 中包含标题和匹配度 |
| T2.4 | 组件 | SourceCard — 空 sources 不渲染 | 组件不输出任何 DOM |
| T2.5 | 组件 | ChatMessage — 用户消息靠右 | CSS class 包含 `user` |
| T2.6 | 组件 | ChatMessage — AI 消息靠左 + 含来源 | CSS class 包含 `assistant`，SourceCard 存在 |
| T2.7 | 组件 | ChatMessage — AI 消息无来源 | 不渲染 SourceCard 区域 |

---

## Phase 3: 问答页面 + 输入组件

### 3.1 目标
组装 QaView 页面，包含 ChatInput 组件和消息列表，实现完整的问答交互。

### 3.2 ChatInput 组件

```
┌─────────────────────────────────────────┬────────┐
│ 输入您的问题...                          │  发送  │
└─────────────────────────────────────────┴────────┘

行为：
- 空内容 → 发送按钮 disabled
- Enter 键 → 发送
- Shift+Enter → 换行
- 发送中 → 按钮显示 loading + 禁用
```

**Emits:** `@send(question: string)`

### 3.3 QaView 页面逻辑

```typescript
// 核心状态
const messages = ref<ChatMessage[]>([])
const loading = ref(false)

async function handleSend(question: string) {
  // 1. 添加用户消息
  messages.value.push({ role: 'user', content: question, ... })
  
  // 2. 调用 API
  loading.value = true
  try {
    const resp = await askQuestion({ question })
    messages.value.push({
      role: 'assistant',
      content: resp.answer,
      intent: resp.intent,
      sources: resp.sources,
      ...
    })
  } catch (e) {
    messages.value.push({
      role: 'assistant',
      content: '抱歉，系统暂时不可用，请稍后重试。',
      ...
    })
  } finally {
    loading.value = false
  }
}
```

### 3.4 页面布局

```
┌──────────────────────────────────────────────────┐
│  🔍 AI 售前助手                                   │
│  ─────────────────────────────────────────────── │
│                                                   │
│  ┌─────────────────────────────────────────────┐ │
│  │                                             │ │
│  │  消息列表（可滚动）                           │ │
│  │  · 用户消息 1                                │ │
│  │  · AI 回复 1                                 │ │
│  │  · 用户消息 2                                │ │
│  │  · AI 回复 2                                 │ │
│  │  · ...                                      │ │
│  │                                             │ │
│  └─────────────────────────────────────────────┘ │
│                                                   │
│  ┌──────────────────────────────────┬──────────┐ │
│  │  输入您的问题...                  │   发送   │ │
│  └──────────────────────────────────┴──────────┘ │
└──────────────────────────────────────────────────┘
```

### 3.5 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T3.1 | 组件 | ChatInput — 空输入时发送按钮 disabled | button disabled=true |
| T3.2 | 组件 | ChatInput — 输入文字后按钮 enabled | button disabled=false |
| T3.3 | 组件 | ChatInput — 点击发送触发 @send 事件 | emitted('send', '输入内容') |
| T3.4 | 组件 | ChatInput — Enter 键触发发送 | emitted('send', ...) |
| T3.5 | 组件 | ChatInput — 发送中显示 loading | button 包含 loading 图标 |
| T3.6 | 集成 | QaView — 发送问题后显示用户消息 | 消息列表新增一条用户消息 |
| T3.7 | 集成 | QaView — API 返回后显示 AI 回复 | 消息列表新增一条 AI 消息 |
| T3.8 | 集成 | QaView — API 错误时显示降级提示 | AI 消息内容为错误提示 |
| T3.9 | E2E  | QaView — 完整问答流程 | 用户输入→发送→看到回答+来源 |

---

## Phase 4: 边缘状态处理

### 4.1 目标
覆盖空状态、加载态、错误态、长文本处理。

### 4.2 状态矩阵

| 状态 | UI 表现 | 触发条件 |
|------|---------|---------|
| 初始空状态 | 中央显示"👋 您好！我是售前助手，可以帮您检索案例、了解产品方案" | 无历史消息 |
| 加载中 | 输入框禁用 + 发送按钮 spinner + AI 消息位显示骨架屏 | API 调用中 |
| API 错误 | AI 消息显示"抱歉，系统暂时不可用" + 红色警告图标 | 网络错误/500 |
| API 超时 | 同上，提示"请求超时，请稍后重试" | 30s 无响应 |
| 无搜索结果 | AI 消息显示"暂无相关信息，建议联系 FA 团队" | sources 为空 |
| 长回答 | 消息区可滚动，不截断内容 | answer > 500 字 |
| 快速连发 | 上一次请求未完成时禁用发送按钮 | loading=true |

### 4.3 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T4.1 | 组件 | QaView — 初始空状态 | 显示欢迎提示，无消息列表 |
| T4.2 | 组件 | ChatInput — 加载中时 disabled | 按钮和输入框都禁用 |
| T4.3 | 集成 | QaView — API 错误降级 | 错误提示 + 用户可重新发送 |
| T4.4 | 集成 | QaView — 无来源时正常显示 | 不显示 SourceCard 区域 |

---

## Phase 5: 端到端集成测试（前后端联调）

### 5.1 目标
前端 + 后端同时启动，验证完整用户链路。

### 5.2 启动流程

```bash
# 终端 1：启动后端
cd yudao-module-ai-bidding
export DEEPSEEK_API_KEY="sk-xxx"
mvn spring-boot:run

# 终端 2：启动前端
cd ai-bidding-frontend
npm run dev

# 浏览器打开 http://localhost:5173
```

### 5.3 E2E 测试场景

| 测试ID | 场景 | 操作 | 期望结果 |
|--------|------|------|---------|
| T5.1 | 正常问答 | 输入"有没有制造业的ERP案例？" → 发送 | 显示回答 + 来源卡片 |
| T5.2 | 打招呼 | 输入"你好" → 发送 | 显示问候语，无来源 |
| T5.3 | 无匹配 | 输入"量子计算解决方案" → 发送 | 显示"暂无相关信息" |
| T5.4 | 连续对话 | 连续发送 3 个不同问题 | 3 轮问答均正常，消息列表可滚动 |
| T5.5 | 后端不可用 | 关闭后端 → 发送问题 | 显示错误提示"系统暂时不可用" |

### 5.4 E2E 测试自动化（可选，Demo 阶段手动验证即可）

```typescript
// e2e/qa.spec.ts (使用 Playwright)
test('完整问答流程', async ({ page }) => {
  await page.goto('http://localhost:5173')
  await page.fill('[data-testid="chat-input"]', '有没有制造业的案例？')
  await page.click('[data-testid="send-button"]')
  await expect(page.locator('[data-testid="message-assistant"]')).toBeVisible()
  await expect(page.locator('[data-testid="source-card"]')).toBeVisible()
})
```

---

## 测试覆盖率汇总

### Phase 完成标准

| 检查项 | 标准 | 工具 |
|--------|------|------|
| 单元/组件测试通过率 | 100% | `npx vitest run` |
| TypeScript 编译 | 0 errors | `npx vue-tsc --noEmit` |
| 开发服务器启动 | 正常 | `npm run dev` |
| E2E 手动验证 | 5 场景全通过 | 浏览器手动测试 |

### 关键测试场景覆盖矩阵

| 场景 | T0.x | T1.x | T2.x | T3.x | T4.x | T5.x |
|------|------|------|------|------|------|------|
| 组件渲染 | ✅ | — | ✅ | — | — | — |
| API 正常返回 | — | ✅ | — | ✅ | — | ✅ |
| API 异常处理 | — | ✅ | — | ✅ | ✅ | — |
| 用户交互（输入/发送） | — | — | — | ✅ | — | — |
| 空/加载/错误状态 | — | — | — | — | ✅ | — |
| 连续对话 | — | — | — | — | — | ✅ |
| 前后端联调 | — | — | — | — | — | ✅ |

---

## AI 执行指令

### 通用规则

```
1. 每个 Phase 开始时 → 读取本 SOP 文档对应 Phase 的任务清单和测试要求
2. 先创建测试文件 → 确认测试列表完整
3. 逐任务编写实现代码 → 运行测试 → 通过后继续
4. 所有测试通过后 → git commit
5. 进入下一个 Phase
```

### 每个 Phase 的执行模板

```
Step 1: 读取 SOP 中 Phase N 的任务清单
Step 2: 创建测试文件（按编号 T{N}.1 ~ T{N}.N）
Step 3: 运行测试 → 确认全部失败（红灯）
Step 4: 创建组件/模块（最小实现）
Step 5: 逐个实现 → 运行测试 → 确认通过（绿灯）
Step 6: git add + git commit
Step 7: 汇报 Phase N 完成状态
```

### 禁止行为

```
❌ 跳过测试直接写业务代码
❌ 一个 Phase 未完成就进入下一个
❌ 测试失败时 commit
❌ 手动修改测试让失败变通过
```

---

## 附录 A: 与后端 SOP 的对应关系

| 后端 Phase | 前端 Phase | 对应关系 |
|-----------|-----------|---------|
| Phase 0 (项目初始化) | Phase 0 (项目初始化) | 各自独立的项目骨架 |
| Phase 1 (LLM API) | Phase 1 (类型+API封装) | 契约定义 |
| Phase 3 (意图识别) | Phase 2 (IntentBadge) | 前端展示意图 |
| Phase 4 (RAG检索) | Phase 2 (SourceCard) | 前端展示来源 |
| Phase 5 (端到端QA) | Phase 3 (QaView) | 前后端联调 |
| Phase 6 (SQL安全) | Phase 4 (边缘状态) | 错误状态处理 |
| — | Phase 5 (E2E) | 端到端验证 |

## 附录 B: Demo 不做的事

| 不做 | 原因 |
|------|------|
| 用户登录/认证 | Demo 阶段不接入 Yudao Security |
| 路由多页面 | Demo 只做一个 QA 页面 |
| 国际化 i18n | Demo 全中文 |
| 暗黑模式 | Demo 不做 |
| 移动端适配 | Demo 桌面端优先 |
| Pinia 状态管理 | 单页面不需要全局 store |
| 需求梳理/方案生成/标书撰写界面 | Demo 只验证技术问答一个模块 |

---

> **文档变更记录**
>
> | 版本 | 日期 | 变更内容 | 作者 |
> |------|------|---------|------|
> | v1.0 | 2026-06-06 | 初稿 — 5 个 Phase + 完整测试矩阵 | AI 辅助生成 |
