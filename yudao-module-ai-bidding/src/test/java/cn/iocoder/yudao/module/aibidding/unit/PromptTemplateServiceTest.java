package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.prompt.PromptNotFoundException;
import cn.iocoder.yudao.module.aibidding.service.prompt.PromptTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class PromptTemplateServiceTest {

    @Autowired
    private PromptTemplateService promptService;

    // T2.1
    @Test
    @DisplayName("T2.1: 加载 intent.classify 模板")
    void shouldLoadIntentClassifyTemplate() {
        String raw = promptService.getRaw("intent.classify");
        assertThat(raw).isNotBlank();
        assertThat(raw).contains("{{question}}");
        assertThat(raw).contains("PRODUCT_QUERY");
    }

    // T2.2
    @Test
    @DisplayName("T2.2: render 替换占位符")
    void shouldRenderTemplateWithParams() {
        String result = promptService.render("intent.classify",
                Map.of("question", "有没有制造业案例？"));
        assertThat(result).doesNotContain("{{question}}");
        assertThat(result).contains("有没有制造业案例？");
    }

    // T2.3
    @Test
    @DisplayName("T2.3: 模板不存在抛异常")
    void shouldThrowExceptionWhenKeyNotFound() {
        assertThatThrownBy(() -> promptService.getRaw("nonexistent.key"))
                .isInstanceOf(PromptNotFoundException.class)
                .hasMessageContaining("nonexistent.key");
    }

    // T2.4
    @Test
    @DisplayName("T2.4: 部分占位符未填充时保留原样")
    void shouldKeepUnfilledPlaceholders() {
        String result = promptService.render("intent.classify", Map.of());
        assertThat(result).contains("{{question}}");
    }
}
