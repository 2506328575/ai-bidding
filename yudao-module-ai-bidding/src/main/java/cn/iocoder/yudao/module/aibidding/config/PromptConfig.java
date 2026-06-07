package cn.iocoder.yudao.module.aibidding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class PromptConfig {

    @Data
    @Configuration
    @ConfigurationProperties(prefix = "aibidding.prompts")
    public static class PromptTemplateList {
        private List<Template> templates = new ArrayList<>();

        @Data
        public static class Template {
            private String key;
            private String content;
        }
    }
}
