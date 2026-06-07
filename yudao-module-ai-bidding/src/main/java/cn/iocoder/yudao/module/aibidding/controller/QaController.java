package cn.iocoder.yudao.module.aibidding.controller;

import cn.iocoder.yudao.module.aibidding.service.llm.ClassificationResult;
import cn.iocoder.yudao.module.aibidding.service.llm.Intent;
import cn.iocoder.yudao.module.aibidding.service.llm.IntentClassifier;
import cn.iocoder.yudao.module.aibidding.service.llm.LlmRequest;
import cn.iocoder.yudao.module.aibidding.service.llm.LlmResponse;
import cn.iocoder.yudao.module.aibidding.service.llm.LlmService;
import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import cn.iocoder.yudao.module.aibidding.service.rag.DocumentChunk;
import cn.iocoder.yudao.module.aibidding.service.rag.KnowledgeService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/qa")
public class QaController {

    private final IntentClassifier intentClassifier;
    private final KnowledgeService knowledgeService;
    private final PromptTemplateService promptService;
    private final LlmService llmService;

    public QaController(IntentClassifier intentClassifier,
                        KnowledgeService knowledgeService,
                        PromptTemplateService promptService,
                        LlmService llmService) {
        this.intentClassifier = intentClassifier;
        this.knowledgeService = knowledgeService;
        this.promptService = promptService;
        this.llmService = llmService;
    }

    @PostMapping("/ask")
    public QaResponse ask(@RequestBody QaRequest request) {
        long start = System.currentTimeMillis();
        String question = request.getQuestion();

        if (question == null || question.isBlank()) {
            return QaResponse.builder()
                    .answer("请输入问题")
                    .intent("GREETING")
                    .sources(List.of())
                    .tokensUsed(0)
                    .latencyMs(0)
                    .build();
        }

        // Step 1: 意图识别
        ClassificationResult classification = intentClassifier.classify(question);
        Intent intent = classification.getIntent();
        log.info("Q&A intent={} path={} question={}", intent, classification.getPath(), question);

        // Step 2: 打招呼直接返回
        if (intent == Intent.GREETING) {
            return QaResponse.builder()
                    .answer("您好！我是售前助手，可以帮您检索案例、查询政策、了解产品技术方案。请问有什么可以帮您的？")
                    .intent("GREETING")
                    .sources(List.of())
                    .tokensUsed(0)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }

        // Step 3: RAG 检索（NL2SQL场景跳过）
        String collection = mapIntentToCollection(intent);
        List<DocumentChunk> retrieved = List.of();
        if (!"__skip_rag__".equals(collection)) {
            retrieved = knowledgeService.search(question, collection, 3);
        }

        // Step 4: 检索无结果降级
        if (retrieved.isEmpty()) {
            return QaResponse.builder()
                    .answer("当前案例库暂无相关信息，建议联系FA团队获取更多支持。")
                    .intent(intent.name())
                    .sources(List.of())
                    .tokensUsed(0)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }

        // Step 5: LLM 生成回答
        try {
            String docsText = retrieved.stream()
                    .map(d -> String.format("[%s] %s", d.getTitle(), d.getContent()))
                    .collect(Collectors.joining("\n---\n"));

            String prompt = promptService.render("qa.rag.answer",
                    Map.of("retrieved_docs", docsText, "question", question));

            LlmResponse llmResponse = llmService.call(LlmRequest.builder()
                    .systemPrompt("你是专业的售前技术助手，回答简洁、准确、专业。")
                    .userMessage(prompt)
                    .maxTokens(800)
                    .build());

            List<QaResponse.Source> sources = retrieved.stream()
                    .map(d -> new QaResponse.Source(d.getTitle(), "relevance: 0.9"))
                    .collect(Collectors.toList());

            return QaResponse.builder()
                    .answer(llmResponse.getContent())
                    .intent(intent.name())
                    .sources(sources)
                    .tokensUsed(llmResponse.getTokensUsed())
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            // Step 5 降级: LLM 失败 → 返回检索原文
            log.warn("LLM调用失败，降级返回检索原文: {}", e.getMessage());
            String fallback = retrieved.stream()
                    .map(d -> String.format("【%s】%s", d.getTitle(), d.getContent()))
                    .collect(Collectors.joining("\n\n"));

            List<QaResponse.Source> sources = retrieved.stream()
                    .map(d -> new QaResponse.Source(d.getTitle(), "relevance: 0.9"))
                    .collect(Collectors.toList());

            return QaResponse.builder()
                    .answer("(LLM暂时不可用，以下是检索到的相关资料)\n\n" + fallback)
                    .intent(intent.name())
                    .sources(sources)
                    .tokensUsed(0)
                    .latencyMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    private String mapIntentToCollection(Intent intent) {
        return switch (intent) {
            case PRODUCT_QUERY -> "product_docs";
            case CASE_RETRIEVAL -> "case_library";
            case POLICY_QUERY -> "case_library";    // Demo: 政策数据未独立部署
            case DATA_QUERY -> "__skip_rag__";      // NL2SQL 不走 RAG
            default -> null;
        };
    }

    @Data
    public static class QaRequest {
        private String question;
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QaResponse {
        private String answer;
        private String intent;
        private List<Source> sources;
        private int tokensUsed;
        private long latencyMs;

        @Data
        @lombok.AllArgsConstructor
        public static class Source {
            private String title;
            private String relevance;
        }
    }
}
