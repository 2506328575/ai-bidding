package cn.iocoder.yudao.module.aibidding.service.llm;

import cn.iocoder.yudao.module.aibidding.config.LlmConfig.LlmProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;

@Slf4j
@Service
public class DeepSeekLlmServiceImpl implements LlmService {

    private final RestTemplate restTemplate;
    private final LlmProperties properties;
    private final ObjectMapper objectMapper;

    public DeepSeekLlmServiceImpl(
            @Qualifier("llmRestTemplate") RestTemplate restTemplate,
            LlmProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public LlmResponse call(LlmRequest request) {
        long start = System.currentTimeMillis();
        int attempts = 0;
        Exception lastException = null;

        while (attempts <= properties.getMaxRetries()) {
            attempts++;
            try {
                return doCall(request, start);
            } catch (LlmApiException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                if (isNetworkError(e) && attempts <= properties.getMaxRetries()) {
                    log.warn("LLM API 调用失败，第{}次重试中... error={}", attempts, e.getMessage());
                    continue;
                }
                throw new LlmApiException("LLM API 调用失败: " + e.getMessage(), e);
            }
        }

        throw new LlmApiException(
                "LLM API 在所有重试后仍失败: " +
                (lastException != null ? lastException.getMessage() : "unknown"));
    }

    private LlmResponse doCall(LlmRequest request, long startTime) throws Exception {
        String url = properties.getBaseUrl() + "/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());

        Map<String, Object> body = buildRequestBody(request);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new LlmApiException("API Key 无效 (401)", 401);
            }

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new LlmApiException(
                        "LLM API 返回错误: " + response.getStatusCode(),
                        response.getStatusCode().value());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choice = root.path("choices").get(0);
            String content = choice.path("message").path("content").asText();
            int tokens = root.path("usage").path("total_tokens").asInt();
            String model = root.path("model").asText("unknown");
            long latency = System.currentTimeMillis() - startTime;

            return LlmResponse.builder()
                    .content(content)
                    .tokensUsed(tokens)
                    .model(model)
                    .latencyMs(latency)
                    .build();

        } catch (LlmApiException e) {
            throw e;
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) {
                throw new LlmApiException("LLM API 调用超时", e);
            }
            if (cause instanceof ConnectException) {
                throw new LlmApiException("无法连接 LLM API: " + cause.getMessage(), e);
            }
            throw new LlmApiException("网络错误: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new LlmApiException("LLM API 解析失败: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> buildRequestBody(LlmRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", request.getTemperature());
        body.put("max_tokens", request.getMaxTokens());

        List<Map<String, String>> messages = new ArrayList<>();

        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of("role", "system", "content", request.getSystemPrompt()));
        }
        messages.add(Map.of("role", "user", "content", request.getUserMessage()));

        body.put("messages", messages);
        return body;
    }

    private boolean isNetworkError(Exception e) {
        if (e instanceof ResourceAccessException) {
            return true;
        }
        return e.getMessage() != null &&
               (e.getMessage().contains("Connection refused") ||
                e.getMessage().contains("connect timed out"));
    }
}
