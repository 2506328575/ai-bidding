package cn.iocoder.yudao.module.aibidding.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmResponse {
    private String content;
    private int tokensUsed;
    private String model;
    private long latencyMs;

    public static LlmResponse empty() {
        return LlmResponse.builder()
                .content("")
                .tokensUsed(0)
                .model("unknown")
                .latencyMs(0)
                .build();
    }
}
