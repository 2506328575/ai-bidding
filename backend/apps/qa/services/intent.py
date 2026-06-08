"""意图分类器 — 关键词快速命中 + LLM 兜底"""
from enum import Enum
from pydantic import BaseModel

class Intent(str, Enum):
    PRODUCT_QUERY = "PRODUCT_QUERY"
    CASE_RETRIEVAL = "CASE_RETRIEVAL"
    POLICY_QUERY = "POLICY_QUERY"
    DATA_QUERY = "DATA_QUERY"
    GREETING = "GREETING"

class ClassificationResult(BaseModel):
    intent: Intent
    path: str  # "KEYWORD" or "LLM"

# 关键词规则
KEYWORD_RULES: dict[Intent, list[str]] = {
    Intent.POLICY_QUERY: ["社保", "公积金", "基数", "比例", "缴纳"],
    Intent.CASE_RETRIEVAL: ["案例", "做过", "类似", "行业", "有没有"],
    Intent.DATA_QUERY: ["合同", "金额", "项目", "名下", "签了"],
    Intent.GREETING: ["你好", "hi", "hello", "早上好"],
    Intent.PRODUCT_QUERY: ["产品", "系统", "支持", "功能", "性能", "配置", "平台"],
}

class IntentClassifier:
    def classify(self, question: str) -> ClassificationResult:
        if not question or not question.strip():
            return ClassificationResult(intent=Intent.GREETING, path="KEYWORD")

        # 关键词快速命中
        lower = question.lower()
        for intent, keywords in KEYWORD_RULES.items():
            if any(kw in lower for kw in keywords):
                return ClassificationResult(intent=intent, path="KEYWORD")

        # 默认：需要 LLM 分类
        return ClassificationResult(intent=Intent.CASE_RETRIEVAL, path="DEFAULT")

    def parse(self, llm_output: str) -> Intent:
        """从 LLM 输出解析意图"""
        if not llm_output:
            return Intent.CASE_RETRIEVAL
        upper = llm_output.strip().upper()
        for intent in Intent:
            if intent.value in upper:
                return intent
        return Intent.CASE_RETRIEVAL
