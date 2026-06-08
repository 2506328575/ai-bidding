"""M1 技术问答 — API 路由"""
from fastapi import APIRouter, Depends
from core.settings import get_settings, Settings
from apps.qa.schemas import ChatRequest, ChatResponse, QaRequest, QaResponse, Source
from apps.qa.services.intent import IntentClassifier, Intent
from apps.qa.services.llm_client import LlmClient, LlmRequest

router = APIRouter(tags=["技术问答"])
classifier = IntentClassifier()

def get_llm(settings: Settings = Depends(get_settings)):
    return LlmClient(
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        model=settings.llm_model,
        connect_timeout=settings.llm_connect_timeout,
        read_timeout=settings.llm_read_timeout,
    )

@router.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, llm: LlmClient = Depends(get_llm)):
    """统一对话入口 — 自动识别意图并路由"""
    cls = classifier.classify(req.message)

    if cls.intent == Intent.GREETING:
        return ChatResponse(type="text", text=_greeting())

    # 其他意图走 QA 流程
    qa_result = await _qa_flow(req.message, cls.intent, llm)
    return ChatResponse(type="qa_result", data=qa_result.model_dump())

@router.post("/qa/ask", response_model=QaResponse)
async def ask(req: QaRequest, llm: LlmClient = Depends(get_llm)):
    """技术问答 — 直接问答"""
    if not req.question or not req.question.strip():
        return QaResponse(answer="请输入问题", intent="GREETING", sources=[])
    cls = classifier.classify(req.question)
    return await _qa_flow(req.question, cls.intent, llm)

async def _qa_flow(question: str, intent: Intent, llm: LlmClient) -> QaResponse:
    """核心 QA 流程"""
    import time
    start = time.time()

    if intent == Intent.GREETING:
        return QaResponse(answer=_greeting(), intent="GREETING", sources=[], latency_ms=int((time.time()-start)*1000))

    # RAG 检索 (Demo 阶段用内存模拟)
    docs = _search_knowledge(question, intent)

    if not docs:
        return QaResponse(
            answer="当前知识库暂无相关信息，建议联系 FA 团队获取更多支持。",
            intent=intent.value, sources=[], latency_ms=int((time.time()-start)*1000))

    # LLM 生成回答
    docs_text = "\n---\n".join(f"[{d['title']}] {d['content']}" for d in docs)
    prompt = f"基于以下资料回答问题。如资料不足请说明。\n\n参考资料：\n{docs_text}\n\n问题：{question}\n\n要求：标注引用来源。"
    try:
        resp = await llm.call(LlmRequest(system_prompt="你是专业的售前技术助手。", user_message=prompt, max_tokens=800))
        return QaResponse(
            answer=resp.content,
            intent=intent.value,
            sources=[Source(title=d["title"], relevance="0.9") for d in docs],
            tokens_used=resp.tokens_used,
            latency_ms=int((time.time()-start)*1000))
    except Exception as e:
        fallback = "\n\n".join(f"【{d['title']}】{d['content']}" for d in docs)
        return QaResponse(
            answer=f"(LLM 暂不可用)\n\n{fallback}", intent=intent.value, sources=[],
            latency_ms=int((time.time()-start)*1000))

def _search_knowledge(question: str, intent: Intent) -> list[dict]:
    """内存知识库检索 (Demo)"""
    docs = {
        "case_library": [
            {"title": "XX制造集团ERP实施案例", "content": "为XX制造集团实施了完整ERP系统，涵盖生产管理/供应链/财务模块。"},
            {"title": "XX银行数据中台项目", "content": "搭建银行统一数据中台，实现实时风控和客户画像分析。"},
            {"title": "XX市政府信创云平台", "content": "基于国产化技术栈建设政务云平台，通过等保三级。"},
            {"title": "XX医院HIS系统升级", "content": "升级三甲医院核心HIS系统，日门诊量5000+。"},
        ],
        "product_docs": [
            {"title": "产品A技术白皮书", "content": "产品A已完成麒麟OS/达梦数据库的全面国产化适配，通过信创目录认证。"},
            {"title": "部署硬件要求", "content": "最低配置: 4C8G/100G SSD；推荐配置: 8C16G/500G SSD，支持K8s。"},
        ]
    }

    collection = "product_docs" if intent == Intent.PRODUCT_QUERY else "case_library"
    pool = docs.get(collection, docs["case_library"])
    # 简单关键词匹配
    matched = [d for d in pool if any(c in d["content"]+d["title"] for c in question)]
    return matched[:3] if matched else []

def _greeting() -> str:
    return ("您好！我是 AI 售前助手。我可以帮您：\n\n"
            "🔍 **检索案例** — \"有没有制造业的ERP案例？\"\n"
            "📄 **生成方案** — \"帮我生成一份制造业售前方案\"\n"
            "📋 **填充标书** — \"用商务标模板生成标书\"\n"
            "💡 **技术问答** — \"你们的系统支持国产化吗？\"")
