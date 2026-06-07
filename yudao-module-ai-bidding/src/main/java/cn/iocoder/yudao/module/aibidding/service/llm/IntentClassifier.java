package cn.iocoder.yudao.module.aibidding.service.llm;

public interface IntentClassifier {
    ClassificationResult classify(String question);
}
