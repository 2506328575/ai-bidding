package cn.iocoder.yudao.module.aibidding.service.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationResult {
    private Intent intent;
    private String path;    // "KEYWORD" or "LLM"
    private long latencyMs;
}
