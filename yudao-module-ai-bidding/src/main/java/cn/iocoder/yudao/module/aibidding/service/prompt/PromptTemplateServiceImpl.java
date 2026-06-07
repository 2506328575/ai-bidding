package cn.iocoder.yudao.module.aibidding.service.prompt;

import cn.iocoder.yudao.module.aibidding.config.PromptConfig.PromptTemplateList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final Map<String, String> templates = new ConcurrentHashMap<>();

    public PromptTemplateServiceImpl(PromptTemplateList config) {
        if (config != null && config.getTemplates() != null) {
            for (var t : config.getTemplates()) {
                templates.put(t.getKey(), t.getContent());
            }
        }
        log.info("加载 Prompt 模板: {} 个", templates.size());
    }

    @Override
    public String render(String key, Map<String, String> params) {
        String template = getRaw(key);
        String result = template;
        for (var entry : params.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            if (result.contains(placeholder)) {
                result = result.replace(placeholder,
                        entry.getValue() != null ? entry.getValue() : "");
            }
        }
        // 检查残留占位符
        int remaining = countRemainingPlaceholders(result);
        if (remaining > 0) {
            log.warn("Prompt 模板 [{}] 存在 {} 个未填充的占位符", key, remaining);
        }
        return result;
    }

    @Override
    public String getRaw(String key) {
        String template = templates.get(key);
        if (template == null) {
            throw new PromptNotFoundException(key);
        }
        return template;
    }

    // 仅测试用
    int getTemplateCount() {
        return templates.size();
    }

    private int countRemainingPlaceholders(String text) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf("{{", idx)) != -1) {
            int end = text.indexOf("}}", idx);
            if (end != -1) {
                count++;
                idx = end + 2;
            } else {
                break;
            }
        }
        return count;
    }
}
