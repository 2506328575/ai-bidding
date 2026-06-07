package cn.iocoder.yudao.module.aibidding.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class LlmConfig {

    @Bean
    @ConfigurationProperties(prefix = "aibidding.llm")
    public LlmProperties llmProperties() {
        return new LlmProperties();
    }

    @Bean("llmRestTemplate")
    public RestTemplate llmRestTemplate(LlmProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.getConnectTimeout().toMillis());
        factory.setReadTimeout((int) props.getReadTimeout().toMillis());
        return new RestTemplate(factory);
    }

    @Data
    public static class LlmProperties {
        private String provider = "deepseek";
        private String apiKey = "";
        private String baseUrl = "https://api.deepseek.com/v1";
        private String model = "deepseek-chat";
        private java.time.Duration connectTimeout = java.time.Duration.ofSeconds(5);
        private java.time.Duration readTimeout = java.time.Duration.ofSeconds(60);
        private int maxRetries = 1;
    }
}
