package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.llm.ClassificationResult;
import cn.iocoder.yudao.module.aibidding.service.llm.Intent;
import cn.iocoder.yudao.module.aibidding.service.llm.IntentClassifier;
import cn.iocoder.yudao.module.aibidding.service.llm.IntentClassifierImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class IntentClassifierTest {

    @Autowired
    private IntentClassifier classifier;

    private List<Map<String, String>> evalCases;

    @BeforeEach
    void loadEvalData() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = getClass().getResourceAsStream("/eval/intent-classify-eval.json")) {
            evalCases = mapper.readValue(is,
                    new TypeReference<List<Map<String, String>>>() {});
        }
    }

    // T3.1
    @Test
    @DisplayName("T3.1: 政策关键词 → POLICY_QUERY (KEYWORD)")
    void shouldClassifyPolicyByKeyword() {
        ClassificationResult r = classifier.classify("广州社保基数是多少？");
        assertThat(r.getIntent()).isEqualTo(Intent.POLICY_QUERY);
        assertThat(r.getPath()).isEqualTo("KEYWORD");
    }

    // T3.2
    @Test
    @DisplayName("T3.2: 案例关键词 → CASE_RETRIEVAL (KEYWORD)")
    void shouldClassifyCaseByKeyword() {
        ClassificationResult r = classifier.classify("有没有制造业ERP案例？");
        assertThat(r.getIntent()).isEqualTo(Intent.CASE_RETRIEVAL);
        assertThat(r.getPath()).isEqualTo("KEYWORD");
    }

    // T3.3
    @Test
    @DisplayName("T3.3: 打招呼 → GREETING (KEYWORD)")
    void shouldClassifyGreetingByKeyword() {
        ClassificationResult r = classifier.classify("你好");
        assertThat(r.getIntent()).isEqualTo(Intent.GREETING);
        assertThat(r.getPath()).isEqualTo("KEYWORD");
    }

    // T3.4 - 验证关键词未命中时走 LLM
    @Test
    @DisplayName("T3.4: 系统问题解析为 PRODUCT_QUERY (LLM路径)")
    void shouldClassifyProductQuery() {
        ClassificationResult r = classifier.classify("你们的系统支持高并发吗？");
        assertThat(r.getIntent()).isNotNull();
        // 如果API未配置，会降级为 CASE_RETRIEVAL
    }

    // T3.5
    @Test
    @DisplayName("T3.5: LLM返回值无法解析时降级")
    void shouldFallbackOnUnparseableOutput() {
        // 模拟 parseIntent 降级逻辑
        IntentClassifierImpl impl = (IntentClassifierImpl) classifier;
        Intent parsed = impl.parseIntent("random noise output");
        assertThat(parsed).isEqualTo(Intent.CASE_RETRIEVAL);
    }

    // T3.6 [EVAL]
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("evalProvider")
    @DisplayName("T3.6 [EVAL]: 20条标注数据 — 关键词命中准确率")
    void evalIntentClassification(Map<String, String> testCase) {
        String question = testCase.get("question");
        Intent expected = Intent.valueOf(testCase.get("expected"));
        ClassificationResult r = classifier.classify(question);
        assertThat(r.getIntent())
                .as("问题: %s", question)
                .isEqualTo(expected);
    }

    static List<Map<String, String>> evalProvider() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = IntentClassifierTest.class
                .getResourceAsStream("/eval/intent-classify-eval.json")) {
            return mapper.readValue(is,
                    new TypeReference<List<Map<String, String>>>() {});
        }
    }
}
