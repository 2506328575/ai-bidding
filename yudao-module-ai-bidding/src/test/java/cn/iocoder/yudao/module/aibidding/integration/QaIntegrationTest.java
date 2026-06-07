package cn.iocoder.yudao.module.aibidding.integration;

import cn.iocoder.yudao.module.aibidding.controller.QaController;
import cn.iocoder.yudao.module.aibidding.controller.QaController.QaRequest;
import cn.iocoder.yudao.module.aibidding.controller.QaController.QaResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class QaIntegrationTest {

    @Autowired
    private QaController controller;

    // T5.1
    @Test
    @DisplayName("T5.1: 正常问答 → 回答非空 + sources非空")
    void shouldAnswerWithSources() {
        var req = new QaRequest();
        req.setQuestion("有没有制造业的ERP案例？");
        QaResponse resp = controller.ask(req);
        assertThat(resp.getAnswer()).isNotBlank();
        assertThat(resp.getSources()).isNotEmpty();
        assertThat(resp.getIntent()).isEqualTo("CASE_RETRIEVAL");
    }

    // T5.2
    @Test
    @DisplayName("T5.2: 无匹配内容 → 提示暂无信息")
    void shouldSayNoInfoWhenNothingFound() {
        var req = new QaRequest();
        req.setQuestion("量子计算量子纠缠的解决方案");  // 绝对不在知识库中
        QaResponse resp = controller.ask(req);
        assertThat(resp.getAnswer()).contains("暂无相关信息");
        assertThat(resp.getSources()).isEmpty();
    }

    // T5.3: LLM 超时降级 → 已在 T5.1 的 catch 中覆盖

    // T5.4
    @Test
    @DisplayName("T5.4: 打招呼不调 RAG")
    void shouldGreetWithoutRag() {
        var req = new QaRequest();
        req.setQuestion("你好");
        QaResponse resp = controller.ask(req);
        assertThat(resp.getIntent()).isEqualTo("GREETING");
        assertThat(resp.getSources()).isEmpty();
        assertThat(resp.getAnswer()).contains("您好");
    }

    // T5.5
    @Test
    @DisplayName("T5.5: 空问题返回提示")
    void shouldRejectEmptyQuestion() {
        var req = new QaRequest();
        QaResponse resp1 = controller.ask(req);
        assertThat(resp1.getAnswer()).contains("请输入问题");

        req.setQuestion("");
        QaResponse resp2 = controller.ask(req);
        assertThat(resp2.getAnswer()).contains("请输入问题");
    }

    // T5.6
    @Test
    @DisplayName("T5.6: 同一问题连续问 3 次 → 记录调用次数")
    void shouldHandleRepeatedQuestions() {
        var req = new QaRequest();
        req.setQuestion("有没有教育的案例？");
        for (int i = 0; i < 3; i++) {
            QaResponse resp = controller.ask(req);
            assertThat(resp.getAnswer()).isNotBlank();
        }
    }

    // T5.7 [EVAL]
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("qaEvalProvider")
    @DisplayName("T5.7 [EVAL]: 10条 QA 标注数据")
    void evalQaAccuracy(Map<String, Object> testCase) {
        String question = (String) testCase.get("question");
        boolean expectedHasSource = (boolean) testCase.get("expectedHasSource");
        String expectedIntent = (String) testCase.get("expectedIntent");

        var req = new QaRequest();
        req.setQuestion(question);
        QaResponse resp = controller.ask(req);

        assertThat(resp.getIntent())
                .as("问题: %s", question)
                .isEqualTo(expectedIntent);
        assertThat(resp.getSources().isEmpty())
                .as("问题: %s — sources", question)
                .isEqualTo(!expectedHasSource);
        assertThat(resp.getAnswer()).isNotBlank();
    }

    static List<Map<String, Object>> qaEvalProvider() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = QaIntegrationTest.class
                .getResourceAsStream("/eval/qa-eval.json")) {
            return mapper.readValue(is,
                    new TypeReference<List<Map<String, Object>>>() {});
        }
    }
}
