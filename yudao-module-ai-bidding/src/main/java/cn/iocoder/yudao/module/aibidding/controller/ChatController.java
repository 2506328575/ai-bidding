package cn.iocoder.yudao.module.aibidding.controller;

import cn.iocoder.yudao.module.aibidding.service.llm.*;
import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import cn.iocoder.yudao.module.aibidding.service.rag.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final LlmService llmService;
    private final IntentClassifier intentClassifier;
    private final KnowledgeService knowledgeService;
    private final PromptTemplateService promptService;
    private final ProposalService proposalService;

    public ChatController(LlmService llmService, IntentClassifier intentClassifier,
                          KnowledgeService knowledgeService, PromptTemplateService promptService,
                          ProposalService proposalService) {
        this.llmService = llmService;
        this.intentClassifier = intentClassifier;
        this.knowledgeService = knowledgeService;
        this.promptService = promptService;
        this.proposalService = proposalService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest req) {
        String userMsg = req.getMessage();
        log.info("Chat: {}", userMsg.substring(0, Math.min(60, userMsg.length())));

        // Step 1: 意图分类
        ClassificationResult cls = intentClassifier.classify(userMsg);
        Intent intent = cls.getIntent();
        log.info("Intent: {} (path={})", intent, cls.getPath());

        // Step 2: 按意图路由
        return switch (intent) {
            case GREETING -> handleGreeting();
            case PRODUCT_QUERY, CASE_RETRIEVAL, POLICY_QUERY -> handleQA(userMsg, intent);
            case DATA_QUERY -> handleQA(userMsg, intent);
        };
    }

    private ChatResponse handleGreeting() {
        return ChatResponse.ok("text",
            "您好！我是 AI 售前助手。我可以帮您：\n\n" +
            "🔍 **检索案例** — \"有没有制造业的ERP案例？\"\n" +
            "📄 **生成方案** — \"帮我生成一份制造业售前方案\"\n" +
            "📋 **填充标书** — \"用商务标模板生成标书\"\n" +
            "💡 **技术问答** — \"你们的系统支持国产化吗？\"\n\n" +
            "请直接告诉我您需要什么。");
    }

    private ChatResponse handleQA(String question, Intent intent) {
        String collection = switch (intent) {
            case PRODUCT_QUERY -> "product_docs";
            case CASE_RETRIEVAL -> "case_library";
            case POLICY_QUERY -> "case_library";
            default -> null;
        };

        if (collection == null) {
            return ChatResponse.ok("text", "数据查询功能开发中，请稍后体验。");
        }

        List<DocumentChunk> retrieved = knowledgeService.search(question, collection, 3);
        if (retrieved.isEmpty()) {
            return ChatResponse.ok("text",
                "当前知识库暂无相关信息，建议联系 FA 团队获取更多支持。");
        }

        try {
            String docs = retrieved.stream()
                .map(d -> "[" + d.getTitle() + "] " + d.getContent())
                .collect(Collectors.joining("\n---\n"));
            String prompt = promptService.render("qa.rag.answer",
                Map.of("retrieved_docs", docs, "question", question));
            LlmResponse resp = llmService.call(LlmRequest.builder()
                .systemPrompt("你是专业的售前技术助手。")
                .userMessage(prompt).maxTokens(800).build());

            List<Map<String, String>> sources = retrieved.stream()
                .map(d -> Map.of("title", d.getTitle(), "relevance", "0.9"))
                .collect(Collectors.toList());

            var data = new HashMap<String, Object>();
            data.put("answer", resp.getContent());
            data.put("sources", sources);
            data.put("tokens", resp.getTokensUsed());
            return ChatResponse.ok("qa_result", data);

        } catch (Exception e) {
            String fallback = retrieved.stream()
                .map(d -> "【" + d.getTitle() + "】" + d.getContent())
                .collect(Collectors.joining("\n\n"));
            var data = new HashMap<String, Object>();
            data.put("answer", "(LLM 暂不可用，以下为检索结果)\n\n" + fallback);
            data.put("sources", List.of());
            return ChatResponse.ok("qa_result", data);
        }
    }

    @Data
    public static class ChatRequest {
        private String message;
    }

    @Data
    public static class ChatResponse {
        private String type;       // text | qa_result
        private String text;
        private Object data;

        public static ChatResponse ok(String type, String text) {
            var r = new ChatResponse(); r.type = type; r.text = text; return r;
        }
        public static ChatResponse ok(String type, Object data) {
            var r = new ChatResponse(); r.type = type; r.data = data; return r;
        }
    }
}
