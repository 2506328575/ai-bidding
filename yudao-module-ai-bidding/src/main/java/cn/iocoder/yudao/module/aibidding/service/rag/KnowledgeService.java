package cn.iocoder.yudao.module.aibidding.service.rag;

import java.util.List;

public interface KnowledgeService {
    List<DocumentChunk> search(String query, String collection, int topK);
    void index(String collection, List<DocumentChunk> documents);
}
