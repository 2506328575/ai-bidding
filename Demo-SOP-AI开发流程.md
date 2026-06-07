# Demo AI 开发 SOP 流程

## 文档说明

本 SOP 定义了 AI 辅助开发 Demo 的标准流程。每个 Phase 包含 **编写代码 → 编写测试 → 验证通过 → 进入下一 Phase** 四个步骤。任何 Phase 的测试未通过，不得进入下一 Phase。

**Demo 目标：** 验证 M1 技术问答模块的核心 AI 链路（LLM API → RAG 检索 → 回答生成）。

**Demo 范围：** 不做完整系统，只做一条核心链路跑通。

```
用户提问 "有没有制造业的ERP案例？"
  → 意图识别 → PRODUCT_QUERY / CASE_RETRIEVAL
  → RAG 检索（从案例库检索相关文档）
  → LLM 结合检索结果生成回答
  → 返回带来源标注的回答
```

**技术栈：** Yudao (Spring Boot) + DeepSeek API + POI-TL + H2 (内存数据库代替 MySQL/Milvus) + JUnit 5

> **AI 执行原则：每个 Phase 遵循 "红灯 → 绿灯 → 重构" 循环。先写测试（红灯），再写代码让测试通过（绿灯），然后优化代码结构（重构）。**

---

## Phase 0: 项目环境初始化

### 0.1 目标
搭建 Yudao 单体项目骨架，确认编译通过、测试框架就绪。

### 0.2 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 0.2.1 | 初始化 Spring Boot 项目（Maven，Java 17+），引入 Yudao 基础依赖 | `mvn compile` 通过 |
| 0.2.2 | 配置 `application.yml`：LLM API endpoint + API key + 日志级别 | 启动无报错 |
| 0.2.3 | 引入测试依赖：JUnit 5 + Mockito + Spring Boot Test + H2 | `mvn test` 跑通一个空测试 |
| 0.2.4 | 创建项目包结构（见下方） | 包结构符合 Yudao 规范 |

### 0.3 包结构

```
yudao-module-ai-bidding/
└── src/
    ├── main/java/cn/iocoder/yudao/module/aibidding/
    │   ├── controller/          # REST API
    │   ├── service/             # 业务逻辑接口 + 实现
    │   │   ├── llm/             # LLM API 调用封装
    │   │   ├── rag/             # RAG 检索（Demo 期用内存模拟）
    │   │   └── prompt/          # Prompt 模板管理
    │   ├── dal/
    │   │   └── dataobject/      # 数据对象
    │   └── config/              # 配置类
    └── test/java/cn/iocoder/yudao/module/aibidding/
        ├── unit/                # 单元测试
        ├── integration/         # 集成测试
        └── eval/                # LLM 质量评估测试
```

### 0.4 测试要求

| 测试类型 | 测试内容 | 框架 |
|---------|---------|------|
| 冒烟测试 | Spring Context 正常加载 | `@SpringBootTest` |

---

## Phase 1: LLM API 调用封装

### 1.1 目标
封装 DeepSeek API（兼容 OpenAI 格式）的 HTTP 调用，支持发送 Prompt 并获取回复。**不依赖任何业务逻辑。**

### 1.2 输入/输出定义

```java
// 输入
public class LlmRequest {
    String systemPrompt;   // 系统指令
    String userMessage;    // 用户消息
    double temperature;    // 0.0-1.0
    int maxTokens;         // 最大输出 token
}

// 输出
public class LlmResponse {
    String content;        // LLM 回复正文
    int tokensUsed;        // 消耗的 token 数
    String model;          // 实际使用的模型名
    long latencyMs;        // 响应延迟
}
```

### 1.3 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 1.3.1 | 创建 `LlmRequest` / `LlmResponse` DTO | 编译通过 |
| 1.3.2 | 创建 `LlmService` 接口 + `DeepSeekLlmServiceImpl`（使用 RestTemplate / WebClient 调用 API） | — |
| 1.3.3 | 创建 `LlmConfig` 配置类（读取 API endpoint / key） | 启动不报错 |
| 1.3.4 | 添加重试机制（1 次失败自动重试） | — |
| 1.3.5 | 添加超时配置（连接 5s，读取 60s） | — |

### 1.4 测试要求

> **测试驱动：先编写测试，再编写实现代码。**

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T1.1 | 单元 | `LlmService.call()` — 正常返回 | `LlmResponse.content` 非空 |
| T1.2 | 单元 | `LlmService.call()` — API Key 无效 | 抛出明确异常，包含状态码 |
| T1.3 | 单元 | `LlmService.call()` — 超时 | 抛出超时异常，不无限阻塞 |
| T1.4 | 单元 | `LlmService.call()` — 网络不可达 | 重试 1 次后抛出异常 |
| T1.5 | 集成 | 真实调用 DeepSeek API，验证返回格式 | 返回内容为中文，tokenUsed > 0 |

```java
// 示例测试结构
@ExtendWith(MockitoExtension.class)
class LlmServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private DeepSeekLlmServiceImpl llmService;

    @Test
    void shouldReturnResponseWhenApiCallSucceeds() {
        // Given: mock API 返回 200 + 有效 JSON
        // When: 调用 llmService.call(request)
        // Then: 返回非空 LlmResponse，content 长度 > 0
    }

    @Test
    void shouldThrowExceptionWhenApiKeyInvalid() {
        // Given: mock API 返回 401
        // When: 调用 llmService.call(request)
        // Then: 抛出 LlmApiException，message 包含 "401"
    }

    @Test
    void shouldThrowTimeoutExceptionWhenReadTimeout() {
        // Given: mock API 延迟超过 60s
        // When: 调用 llmService.call(request)
        // Then: 抛出超时异常
    }

    @Test
    void shouldRetryOnceThenThrowOnNetworkError() {
        // Given: mock API 两次都抛 ConnectException
        // When: 调用 llmService.call(request)
        // Then: 抛出异常，verify restTemplate 被调用 2 次
    }
}
```

---

## Phase 2: Prompt 模板管理

### 2.1 目标
实现 Prompt 与代码分离。Demo 阶段用 YAML 配置文件存储，预留数据库接口。

### 2.2 输入/输出定义

```java
// 模板 key + 占位符参数 → 渲染后的完整 Prompt
public interface PromptTemplateService {
    String render(String key, Map<String, String> params);
    String getRaw(String key);  // 获取原始模板（调试用）
}
```

### 2.3 配置格式（`application.yml`）

```yaml
aibidding:
  prompts:
    templates:
      - key: intent.classify
        content: |
          判断用户问题意图，只返回以下之一：
          PRODUCT_QUERY | CASE_RETRIEVAL | POLICY_QUERY | DATA_QUERY | GREETING
          
          示例：
          "有没有制造业案例" → CASE_RETRIEVAL
          "系统支持信创吗" → PRODUCT_QUERY
          "广州社保基数" → POLICY_QUERY
          "我名下几个项目" → DATA_QUERY
          
          用户问题：{{question}}
      - key: qa.rag.answer
        content: |
          基于以下资料回答问题。如果资料不足以回答，请说"当前案例库暂无相关信息"。
          
          参考资料：
          {{retrieved_docs}}
          
          问题：{{question}}
          
          要求：回答末尾标注引用来源。
```

### 2.4 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 2.4.1 | 创建 `PromptTemplateServiceImpl` — 从 YAML 读取模板，支持 `{{key}}` 占位符替换 | 编译通过 |
| 2.4.2 | 创建 `PromptConfig` — 将 YAML 配置注入为 `List<PromptTemplate>` Bean | 启动加载无报错 |
| 2.4.3 | 支持模板不存在时抛出明确异常 | — |

### 2.5 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T2.1 | 单元 | 加载 `intent.classify` 模板 | 返回非空字符串，含 `{{question}}` 占位符 |
| T2.2 | 单元 | `render("intent.classify", {"question": "测试"})` | 返回内容不含 `{{question}}`，含"测试" |
| T2.3 | 单元 | 渲染不存在的 key | 抛出 `PromptNotFoundException` |
| T2.4 | 单元 | 部分占位符未填充 | 保留原样 `{{unfilled}}` 并 WARN 日志（不抛异常） |

---

## Phase 3: 意图识别

### 3.1 目标
实现两级意图分类：关键词快速命中（不调 LLM）+ LLM 分类（慢路径兜底）。

### 3.2 输入/输出定义

```java
public enum Intent {
    PRODUCT_QUERY,    // 产品技术咨询
    CASE_RETRIEVAL,   // FA 案例检索
    POLICY_QUERY,     // 政策法规问题
    DATA_QUERY,       // 业务数据查询
    GREETING          // 打招呼
}

// 输入：用户自然语言问题
// 输出：分类结果
public class ClassificationResult {
    Intent intent;
    String path;           // "KEYWORD" 或 "LLM"
    long latencyMs;
}
```

### 3.3 关键词命中规则（硬编码配置）

```java
// 配置在 application.yml 或常量类中
Map<Intent, List<String>> KEYWORD_RULES = Map.of(
    POLICY_QUERY,   List.of("社保", "公积金", "基数", "比例", "缴纳"),
    CASE_RETRIEVAL, List.of("案例", "做过", "类似", "行业", "有没有"),
    DATA_QUERY,     List.of("合同", "金额", "项目", "多少", "名下"),
    GREETING,       List.of("你好", "hi", "hello")
);
```

### 3.4 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 3.4.1 | 创建 `IntentClassifier` 接口 + 实现（关键词匹配 + LLM 分类） | — |
| 3.4.2 | 实现关键词快速路径：遍历规则，命中即返回 | 10 条标准 query 全命中 |
| 3.4.3 | 实现 LLM 分类路径：调用 `PromptTemplateService.render("intent.classify")` → `LlmService.call()` → 解析返回值 | Intent 枚举解析正确 |
| 3.4.4 | LLM 返回值无法匹配枚举时 → 降级为 `CASE_RETRIEVAL`（最通用的类型） | 不抛异常 |

### 3.5 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T3.1 | 单元 | "广州社保基数是多少？" | 关键词命中 → `POLICY_QUERY`，path="KEYWORD" |
| T3.2 | 单元 | "有没有制造业ERP案例？" | 关键词命中 → `CASE_RETRIEVAL`，path="KEYWORD" |
| T3.3 | 单元 | "你好" | 关键词命中 → `GREETING`，path="KEYWORD" |
| T3.4 | 单元 | "你们的系统支持高并发吗？"（关键词未命中） | 走 LLM 分类 → 期望返回 `PRODUCT_QUERY` |
| T3.5 | 单元 | LLM 返回无法解析的值 | 降级为 `CASE_RETRIEVAL`，WARN 日志 |
| T3.6 | **[EVAL]** | 预设 20 条标注 query，计算关键词命中率 + LLM 分类准确率 | 关键词命中率无要求（有多少算多少），LLM 分类准确率 ≥ 90% |

**Eval 测试数据结构：**

```java
// eval/intent-classify-eval.json (测试数据文件)
[
  {"question": "广州社保基数是多少？", "expected": "POLICY_QUERY"},
  {"question": "有没有制造业的案例？", "expected": "CASE_RETRIEVAL"},
  {"question": "你们的系统支持国产化吗？", "expected": "PRODUCT_QUERY"},
  {"question": "去年广东签了多少合同？", "expected": "DATA_QUERY"},
  {"question": "你好", "expected": "GREETING"},
  // ... 共 20 条
]
```

```java
@ParameterizedTest
@JsonFileSource(resource = "/eval/intent-classify-eval.json")
void evalIntentClassification(TestCase tc) {
    ClassificationResult result = classifier.classify(tc.question);
    assertThat(result.getIntent()).isEqualTo(tc.expectedIntent());
}
```

---

## Phase 4: RAG 检索（Demo 内存版）

### 4.1 目标
Demo 阶段用内存模拟向量检索——预设 10 条案例文档，用简单的关键词匹配代替 Embedding 相似度，验证 RAG → LLM 链路。**预留 `KnowledgeService` 接口，后续替换为 Milvus 实现。**

### 4.2 输入/输出定义

```java
// 检索接口（Demo 内存实现 + 未来 Milvus 实现）
public interface KnowledgeService {
    List<DocumentChunk> search(String query, String collection, int topK);
    void index(String collection, List<DocumentChunk> documents);
}

public class DocumentChunk {
    String id;
    String title;
    String content;
    String source;          // 来源文件
    Map<String, String> metadata;  // 行业/类型/日期等
}
```

### 4.3 Demo 预设案例数据（10 条，嵌入在代码中）

| 标题 | 行业 | 类型 |
|------|------|------|
| XX 制造集团 ERP 实施案例 | 制造业 | 案例 |
| XX 银行数据中台项目 | 金融 | 案例 |
| XX 市政府信创云平台 | 政府 | 案例 |
| XX 医院 HIS 系统升级 | 医疗 | 案例 |
| XX 大学智慧校园项目 | 教育 | 案例 |
| 产品 A 技术白皮书（信创适配） | — | 产品文档 |
| 产品 B 功能列表 v3.2 | — | 产品文档 |
| 项目部署硬件要求 | — | 产品文档 |
| XX 制造集团 ERP 二期合同（中标） | 制造业 | 历史标书 |
| XX 市政府信创云平台验收报告 | 政府 | 历史标书 |

### 4.4 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 4.4.1 | 创建 `DocumentChunk` 数据类 + `KnowledgeService` 接口 | 编译通过 |
| 4.4.2 | 创建 `InMemoryKnowledgeService` — 用 TF-IDF 关键词匹配模拟检索 | 10 条数据加载成功 |
| 4.4.3 | 实现 `search(query, collection, topK)` — 返回 Top-3 匹配 | 返回结果按相关性排序 |
| 4.4.4 | 支持按 collection 过滤（`case_library` / `product_docs`） | — |

### 4.5 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T4.1 | 单元 | "制造业 ERP" → 搜索 case_library | Top-1 为 "XX 制造集团 ERP 实施案例" |
| T4.2 | 单元 | "信创" → 搜索 product_docs | Top-1 包含 "信创" |
| T4.3 | 单元 | "区块链" → 搜索所有集合 | 返回空列表（非异常） |
| T4.4 | 单元 | `search("test", "case_library", 3)` | 返回 ≤ 3 条结果 |
| T4.5 | 单元 | 空查询 `search("", ...)` | 返回空列表，不抛异常 |

---

## Phase 5: 问答端到端集成

### 5.1 目标
把 Phase 1-4 的组件串联：用户提问 → 意图识别 → RAG 检索 → LLM 生成回答 → 返回给用户。

### 5.2 REST API 定义

```java
// POST /api/v1/qa/ask
// Request:
{
    "question": "有没有制造业的ERP案例？"
}

// Response:
{
    "answer": "根据案例库资料，我们曾为 XX 制造集团实施过 ERP 项目...（来源：XX 制造集团 ERP 实施案例）",
    "intent": "CASE_RETRIEVAL",
    "sources": [
        {"title": "XX 制造集团 ERP 实施案例", "relevance": 0.92}
    ],
    "tokensUsed": 450,
    "latencyMs": 2300
}
```

### 5.3 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 5.3.1 | 创建 `QaController` + `QaService` 接口 | — |
| 5.3.2 | 实现 `QaServiceImpl.ask(question)` — 串联 4 个 Phase | — |
| 5.3.3 | 实现降级策略：RAG 检索无结果 → 提示"暂无相关信息"（不编造） | — |
| 5.3.4 | 实现降级策略：LLM 调用失败 → 返回检索结果原文（不调 LLM） | — |
| 5.3.5 | 添加请求/响应日志（记录 question / intent / latency / tokens） | — |

### 5.4 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T5.1 | 集成 | `POST /api/v1/qa/ask` — 正常问答 | HTTP 200，answer 非空，sources 非空 |
| T5.2 | 集成 | 提问无匹配案例（"区块链相关案例"） | answer 含"暂无相关信息"，sources 为空 |
| T5.3 | 集成 | LLM 超时降级 | 返回检索结果原文，HTTP 200（非 500） |
| T5.4 | 单元 | `QaService` — 打招呼 | intent=GREETING，不调 RAG，直接返回问候语 |
| T5.5 | 单元 | `QaService` — 空问题 `null` / `""` | 返回"请输入问题"，不调 LLM |
| T5.6 | 单元 | 同一问题连续问 3 次（验证后续需加入的 LLM 缓存逻辑，Demo 期先做调用计数） | 验证被调用的次数，记录日志 |
| T5.7 | **[EVAL]** | 预设 10 条标注 QA pairs，人工评估回答质量 | 相关性 ≥ 3/5，准确性 ≥ 4/5，来源准确性 ≥ 4/5 |

**Eval 数据结构：**

```java
// eval/qa-eval.json
[
  {
    "question": "有没有制造业的ERP案例？",
    "expectedHasSource": true,
    "expectedIntent": "CASE_RETRIEVAL",
    "minAnswerLength": 20
  },
  {
    "question": "你们的系统支持信创吗？",
    "expectedHasSource": true,
    "expectedIntent": "PRODUCT_QUERY",
    "minAnswerLength": 15
  },
  // ... 共 10 条
]
```

---

## Phase 6: NL2SQL 安全层（仅骨架）

### 6.1 目标
Demo 期不做真实 NL2SQL，只实现 SQL 安全拦截器骨架——验证未来 SQL 执行沙箱的设计可行。

### 6.2 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 6.2.1 | 创建 `SqlSafetyInterceptor` — 拦截包含 DROP/DELETE/UPDATE/INSERT/ALTER 的 SQL | — |
| 6.2.2 | 配置只读数据源（Demo 用 H2 只读账号） | 写操作抛出权限异常 |

### 6.3 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T6.1 | 单元 | 传入 `SELECT * FROM contract` | 通过 |
| T6.2 | 单元 | 传入 `DROP TABLE contract` | 抛出 `SqlSafetyException` |
| T6.3 | 单元 | 传入 `DELETE FROM contract` | 抛出 `SqlSafetyException` |
| T6.4 | 单元 | 传入 `UPDATE contract SET ...` | 抛出 `SqlSafetyException` |

---

## Phase 7: Word 模板填充 Demo（POI-TL 验证）

### 7.1 目标
使用现有 `技术标——模板(1).docx`，验证 POI-TL 模板填充能力。Demo 不接 AI，只用硬编码数据填入占位符。

### 7.2 任务清单

| # | 任务 | 验证标准 |
|---|------|---------|
| 7.2.1 | 读取模板文件，识别所有占位符 `{{xxx}}` | 输出占位符列表 |
| 7.2.2 | 用预设数据填充模板并生成新文件 | 生成 docx 可用 Word 打开 |
| 7.2.3 | 验证填充结果：所有占位符被替换，无残留 | 遍历段落检查 |

### 7.3 测试要求

| 测试ID | 测试类型 | 测试内容 | 期望结果 |
|--------|---------|---------|---------|
| T7.1 | 单元 | 填充模板后读取段落 | 不含 `{{` 子串 |
| T7.2 | 单元 | 模板文件不存在 | 抛出明确异常 |
| T7.3 | 单元 | 未填充的占位符（参数不完整） | 保留 `{{unfilled}}` 原样 + WARN |

---

## 测试覆盖率汇总

### Phase 完成标准

每个 Phase 完成前必须通过以下检查：

| 检查项 | 标准 | 工具 |
|--------|------|------|
| 单元测试通过率 | 100% | `mvn test` |
| 新增代码行覆盖率 | ≥ 80% | JaCoCo |
| Eval 测试（标注数据集） | ≥ 90% 准确率 | 自定义 eval harness |
| 编译无警告 | 0 warnings | `mvn compile` |

### 关键测试场景覆盖矩阵

| 场景 | T1.x | T2.x | T3.x | T4.x | T5.x | T6.x | T7.x |
|------|------|------|------|------|------|------|------|
| 正常路径 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| API 不可达 | ✅ | — | — | — | ✅ | — | — |
| 参数为空/无效 | — | ✅ | ✅ | ✅ | ✅ | — | ✅ |
| LLM 超时 | ✅ | — | — | — | ✅ | — | — |
| LLM 返回无法解析 | — | — | ✅ | — | — | — | — |
| 检索无结果 | — | — | — | ✅ | ✅ | — | — |
| SQL 注入 | — | — | — | — | — | ✅ | — |
| 资源不存在 | — | ✅ | — | — | — | — | ✅ |

---

## AI 执行指令

以下指令供 AI（Claude Code）在每个 Phase 中严格执行：

### 通用规则

```
1. 每个 Phase 开始时 → 读取本 SOP 文档对应 Phase 的任务清单和测试要求
2. 先创建测试类 + 测试方法（标记 @Disabled 或写空实现）→ 确认测试列表完整
3. 逐任务编写实现代码 → 启用对应测试 → 运行测试 → 通过后继续
4. 所有测试通过后 → git commit（WIP 格式）
5. 进入下一个 Phase
```

### 每个 Phase 的执行模板

```
AI 执行 Phase N 的流程：

Step 1: 读取 SOP 中 Phase N 的任务清单
Step 2: 创建测试文件（按照 T{N}.1 ~ T{N}.N 编号创建测试方法）
Step 3: 运行测试 → 确认全部失败（红灯）
Step 4: 创建接口/数据类（最小实现）
Step 5: 创建实现类 → 逐个实现方法
Step 6: 逐条运行测试 → 确认通过（绿灯）
Step 7: 如有需要 → 重构代码（保持测试绿）
Step 8: git add + git commit（WIP: Phase N — <描述>）
Step 9: 汇报 Phase N 完成状态：
    - 测试数量：X 通过 / Y 总计
    - 覆盖率：X%
    - 关键决策：<本 Phase 中的技术决策>
```

### 禁止行为

```
❌ 跳过测试直接写业务代码
❌ 一个 Phase 未完成就进入下一个
❌ 测试失败时 commit
❌ 手动修改测试用例让失败变通过（除非确认测试用例本身写错了）
❌ 删除或注释掉失败测试
```

---

## 附录 A: Demo 环境要求

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 17+ | — |
| Maven | 3.8+ | — |
| Spring Boot | 3.x（Yudao 默认） | — |
| DeepSeek API Key | — | 或任何 OpenAI 兼容的 API endpoint |
| H2 Database | 最新 | 内存模式，Demo 用 |

## 附录 B: Demo 不做的事

| 不做 | 原因 |
|------|------|
| Milvus 真实部署 | Demo 用内存模拟，验证接口设计正确即可 |
| 前端页面 | Demo 只验证后端 AI 链路，用 curl/Postman 测试 |
| 用户认证 | 复用 Yudao 默认安全配置，Demo 不自定义 |
| 数据库 MySQL | H2 内存库替代 |
| 完整 NL2SQL | 只做 SQL 安全拦截器骨架 |
| PPT 填充 | 只做 Word（POI-TL），PPT 逻辑类似 |
| 多租户 | Demo 单租户 |
| 政策抓取 | Demo 不做（依赖外部 URL 和定时任务） |

---

> **文档变更记录**
>
> | 版本 | 日期 | 变更内容 | 作者 |
> |------|------|---------|------|
> | v1.0 | 2026-06-06 | 初稿 — 7 个 Phase + 完整测试矩阵 | AI 辅助生成 |
