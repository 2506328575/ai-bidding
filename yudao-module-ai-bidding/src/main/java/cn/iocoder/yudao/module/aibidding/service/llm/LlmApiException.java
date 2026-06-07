package cn.iocoder.yudao.module.aibidding.service.llm;

public class LlmApiException extends RuntimeException {
    private final int statusCode;

    public LlmApiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public LlmApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LlmApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
