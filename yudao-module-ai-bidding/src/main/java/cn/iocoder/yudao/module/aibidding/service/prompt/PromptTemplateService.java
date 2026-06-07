package cn.iocoder.yudao.module.aibidding.service.prompt;

import java.util.Map;

public interface PromptTemplateService {
    String render(String key, Map<String, String> params);
    String getRaw(String key);
}
