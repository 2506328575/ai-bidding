package cn.iocoder.yudao.module.aibidding.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmRequest {
    private String systemPrompt;
    private String userMessage;
    @Builder.Default
    private double temperature = 0.7;
    @Builder.Default
    private int maxTokens = 2000;
}
