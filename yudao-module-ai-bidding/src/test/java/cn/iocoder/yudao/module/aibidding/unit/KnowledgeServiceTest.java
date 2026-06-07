package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.rag.DocumentChunk;
import cn.iocoder.yudao.module.aibidding.service.rag.KnowledgeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;

    // T4.1
    @Test
    @DisplayName("T4.1: 「制造业 ERP」检索 case_library → Top-1 为制造业案例")
    void shouldFindManufacturingCase() {
        List<DocumentChunk> results = knowledgeService.search("制造业 ERP", "case_library", 3);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getTitle()).contains("制造");
    }

    // T4.2
    @Test
    @DisplayName("T4.2: 「信创」检索 product_docs → Top-1 包含信创")
    void shouldFindXinchuangDoc() {
        List<DocumentChunk> results = knowledgeService.search("信创", "product_docs", 3);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getContent()).contains("信创");
    }

    // T4.3
    @Test
    @DisplayName("T4.3: 「区块链」无匹配 → 返回空列表")
    void shouldReturnEmptyForUnmatchedQuery() {
        List<DocumentChunk> results = knowledgeService.search("区块链", "case_library", 3);
        assertThat(results).isEmpty();
    }

    // T4.4
    @Test
    @DisplayName("T4.4: topK=3 返回 ≤3 条")
    void shouldRespectTopK() {
        List<DocumentChunk> results = knowledgeService.search("项目", null, 3);
        assertThat(results).hasSizeLessThanOrEqualTo(3);
    }

    // T4.5
    @Test
    @DisplayName("T4.5: 空查询返回空列表")
    void shouldReturnEmptyForBlankQuery() {
        assertThat(knowledgeService.search("", "case_library", 3)).isEmpty();
        assertThat(knowledgeService.search(null, "case_library", 3)).isEmpty();
    }
}
