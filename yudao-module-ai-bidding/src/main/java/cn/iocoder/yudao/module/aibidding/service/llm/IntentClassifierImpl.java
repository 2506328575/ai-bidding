package cn.iocoder.yudao.module.aibidding.service.llm;

import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class IntentClassifierImpl implements IntentClassifier {

    private final PromptTemplateService promptService;
    private final LlmService llmService;

    public IntentClassifierImpl(PromptTemplateService promptService, LlmService llmService) {
        this.promptService = promptService;
        this.llmService = llmService;
    }

    @Override
    public ClassificationResult classify(String question) {
        long start = System.currentTimeMillis();

        if (question == null || question.isBlank()) {
            return new ClassificationResult(Intent.GREETING, "KEYWORD", 0);
        }

        // 第一层：关键词快速命中
        Intent keywordIntent = keywordMatch(question);
        if (keywordIntent != null) {
            return new ClassificationResult(keywordIntent, "KEYWORD",
                    System.currentTimeMillis() - start);
        }

        // 第二层：LLM 分类
        try {
            String prompt = promptService.render("intent.classify",
                    Map.of("question", question));
            LlmResponse response = llmService.call(LlmRequest.builder()
                    .userMessage(prompt)
                    .maxTokens(20)
                    .temperature(0.1)
                    .build());
            Intent llmIntent = parseIntent(response.getContent());
            return new ClassificationResult(llmIntent, "LLM",
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.warn("LLM 意图分类失败，降级为 CASE_RETRIEVAL: {}", e.getMessage());
            return new ClassificationResult(Intent.CASE_RETRIEVAL, "KEYWORD",
                    System.currentTimeMillis() - start);
        }
    }

    private Intent keywordMatch(String question) {
        String lower = question.toLowerCase();
        if (containsAny(lower, List.of("社保", "公积金", "基数", "比例", "缴纳")))
            return Intent.POLICY_QUERY;
        if (containsAny(lower, List.of("案例", "做过", "类似", "行业", "有没有")))
            return Intent.CASE_RETRIEVAL;
        if (containsAny(lower, List.of("合同", "金额", "项目", "名下", "签了")))
            return Intent.DATA_QUERY;
        if (containsAny(lower, List.of("你好", "hi", "hello", "早上好")))
            return Intent.GREETING;
        if (containsAny(lower, List.of("产品", "系统", "支持", "功能", "性能", "配置", "平台")))
            return Intent.PRODUCT_QUERY;
        return null;
    }

    private boolean containsAny(String text, List<String> keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    public Intent parseIntent(String llmOutput) {
        if (llmOutput == null) return Intent.CASE_RETRIEVAL;
        String trimmed = llmOutput.trim().toUpperCase();
        for (Intent intent : Intent.values()) {
            if (trimmed.contains(intent.name())) return intent;
        }
        log.warn("LLM 返回无法解析的意图: {}", llmOutput);
        return Intent.CASE_RETRIEVAL;
    }
}
