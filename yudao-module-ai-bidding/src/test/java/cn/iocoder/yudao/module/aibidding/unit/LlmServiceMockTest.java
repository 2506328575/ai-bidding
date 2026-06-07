package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.llm.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LLM Service 单元测试（Mock）")
class LlmServiceMockTest {

    @Mock
    private LlmService llmService;

    private LlmRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = LlmRequest.builder()
                .systemPrompt("你是一个售前助手。")
                .userMessage("测试问题")
                .maxTokens(50)
                .build();
    }

    // T1.1
    @Test
    @DisplayName("T1.1: 正常LLM调用返回非空内容")
    void shouldReturnResponseWhenApiCallSucceeds() {
        LlmResponse mockResponse = LlmResponse.builder()
                .content("Java 是一种面向对象的编程语言。")
                .tokensUsed(42)
                .model("deepseek-chat")
                .latencyMs(1234)
                .build();
        when(llmService.call(validRequest)).thenReturn(mockResponse);

        LlmResponse response = llmService.call(validRequest);
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isNotBlank();
        assertThat(response.getTokensUsed()).isGreaterThan(0);
        assertThat(response.getLatencyMs()).isGreaterThan(0);
    }

    // T1.2
    @Test
    @DisplayName("T1.2: API Key无效时抛出LlmApiException")
    void shouldThrowExceptionWhenApiKeyInvalid() {
        when(llmService.call(any()))
                .thenThrow(new LlmApiException("API Key 无效 (401)", 401));

        assertThatThrownBy(() -> llmService.call(validRequest))
                .isInstanceOf(LlmApiException.class)
                .hasMessageContaining("401");
    }

    // T1.3
    @Test
    @DisplayName("T1.3: 超时抛出异常")
    void shouldThrowTimeoutExceptionWhenReadTimeout() {
        when(llmService.call(any()))
                .thenThrow(new LlmApiException("LLM API 调用超时"));

        assertThatThrownBy(() -> llmService.call(validRequest))
                .isInstanceOf(LlmApiException.class)
                .hasMessageContaining("超时");
    }

    // T1.4
    @Test
    @DisplayName("T1.4: 网络错误抛出异常")
    void shouldThrowExceptionOnNetworkError() {
        when(llmService.call(any()))
                .thenThrow(new LlmApiException("无法连接 LLM API: Connection refused"));

        assertThatThrownBy(() -> llmService.call(validRequest))
                .isInstanceOf(LlmApiException.class)
                .hasMessageContaining("无法连接");
    }
}
