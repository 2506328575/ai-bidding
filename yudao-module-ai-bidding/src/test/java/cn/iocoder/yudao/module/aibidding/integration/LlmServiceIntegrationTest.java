package cn.iocoder.yudao.module.aibidding.integration;

import cn.iocoder.yudao.module.aibidding.service.llm.LlmRequest;
import cn.iocoder.yudao.module.aibidding.service.llm.LlmResponse;
import cn.iocoder.yudao.module.aibidding.service.llm.LlmService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("LLM API 集成测试（需真实 API Key）")
class LlmServiceIntegrationTest {

    @Autowired
    private LlmService llmService;

    @Test
    @DisplayName("T1.5: 真实调用 DeepSeek API，验证返回格式")
    void shouldReturnValidResponseFromRealApi() {
        LlmRequest request = LlmRequest.builder()
                .systemPrompt("你是一个售前助手，回答简洁专业。")
                .userMessage("请用一句话介绍Java")
                .maxTokens(100)
                .build();

        LlmResponse response = llmService.call(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getTokensUsed()).isGreaterThan(0);
        assertThat(response.getModel()).isNotBlank();
        assertThat(response.getLatencyMs()).isGreaterThan(0)
                .isLessThan(10_000);
    }
}
