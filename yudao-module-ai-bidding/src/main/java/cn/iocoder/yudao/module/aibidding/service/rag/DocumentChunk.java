package cn.iocoder.yudao.module.aibidding.service.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {
    private String id;
    private String title;
    private String content;
    private String source;
    private Map<String, String> metadata;
}
