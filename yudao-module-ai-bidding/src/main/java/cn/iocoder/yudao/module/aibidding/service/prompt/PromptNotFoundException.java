package cn.iocoder.yudao.module.aibidding.service.prompt;

public class PromptNotFoundException extends RuntimeException {
    public PromptNotFoundException(String key) {
        super("Prompt 模板不存在: " + key);
    }
}
