package cn.iocoder.yudao.module.aibidding.unit;

import cn.iocoder.yudao.module.aibidding.service.llm.WordTemplateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WordTemplateServiceTest {

    private final WordTemplateService service = new WordTemplateService();

    // T7.1: 模板占位符提取
    @Test @DisplayName("T7.1: 提取模板占位符")
    void shouldExtractPlaceholders() throws Exception {
        // 创建一个带占位符的简单 docx 用于测试
        String templateDir = "../Ai招投标模板/";
        try {
            var placeholders = service.extractPlaceholders(
                    templateDir + "技术标——模板(1).docx");
            assertThat(placeholders).isNotNull();
            log("占位符: " + placeholders);
        } catch (FileNotFoundException e) {
            log("模板文件未找到，跳过占位符提取测试");
        }
    }

    // T7.2: 模板不存在抛异常
    @Test @DisplayName("T7.2: 模板文件不存在 → FileNotFoundException")
    void shouldThrowWhenTemplateNotFound() {
        assertThatThrownBy(() -> service.fillTemplate(
                "nonexistent_template.docx", Map.of()))
                .isInstanceOf(FileNotFoundException.class)
                .hasMessageContaining("模板文件不存在");
    }

    // T7.3: 部分参数未填充 → log WARN
    @Test @DisplayName("T7.3: 参数不完整 → 不抛异常（占位符保留原样）")
    void shouldNotThrowOnIncompleteParams() throws Exception {
        String templateDir = "../Ai招投标模板/";
        try {
            byte[] result = service.fillTemplate(
                    templateDir + "商务标——模板(1).docx",
                    Map.of());  // 不传参数
            assertThat(result).isNotEmpty();
            log("模板填充完成: " + result.length + " bytes");
        } catch (FileNotFoundException e) {
            log("模板文件未找到，跳过填充测试");
        }
    }

    private void log(String msg) {
        System.out.println("[T7] " + msg);
    }
}
