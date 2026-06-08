"""M2 需求梳理 — API"""
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import Optional
from apps.qa.services.llm_client import LlmClient, LlmRequest
from core.settings import get_settings
import uuid, json

router = APIRouter(prefix="/requirement", tags=["需求梳理"])

# 维度配置 (Demo硬编码，生产从DB加载)
DIMENSIONS = [
    {"name": "行业", "type": "ENUM", "options": ["制造业","金融","政府","医疗","教育","互联网","其他"], "required": True, "sort": 1},
    {"name": "规模", "type": "ENUM", "options": ["50人以下","50-200人","200-500人","500-2000人","2000人以上"], "required": True, "sort": 2},
    {"name": "核心痛点", "type": "TEXTAREA", "options": None, "required": True, "sort": 3},
    {"name": "预算范围", "type": "ENUM", "options": ["50万以下","50-200万","200-500万","500万以上"], "required": True, "sort": 4},
    {"name": "交付时限", "type": "DATE", "options": None, "required": True, "sort": 5},
    {"name": "已有系统", "type": "TEXT", "options": None, "required": False, "sort": 6},
]

# 会话存储
_sessions: dict[str, dict] = {}

class SessionStartResponse(BaseModel):
    session_id: str
    current_dimension: str
    question: str

class SessionAnswer(BaseModel):
    session_id: str
    answer: str

class SessionState(BaseModel):
    session_id: str
    dimensions: list[dict]
    current_index: int
    finished: bool

@router.get("/dimensions")
async def list_dimensions():
    """获取当前生效的需求采集维度"""
    return {"dimensions": DIMENSIONS}

@router.post("/session/start", response_model=SessionStartResponse)
async def start_session():
    """开启一个新的需求梳理会话"""
    sid = uuid.uuid4().hex[:8]
    dims = [{**d, "value": None, "status": "pending"} for d in DIMENSIONS]
    dims[0]["status"] = "active"
    _sessions[sid] = {"dimensions": dims, "current_index": 0, "messages": []}
    d = dims[0]
    return SessionStartResponse(
        session_id=sid,
        current_dimension=d["name"],
        question=f"请描述您的{d['name']}情况" + (f"（可选: {', '.join(d['options'])}）" if d["options"] else "")
    )

@router.post("/session/answer", response_model=SessionStartResponse)
async def answer_session(req: SessionAnswer, settings=Depends(get_settings)):
    """用户回答当前维度 → AI追问下一个维度"""
    session = _sessions.get(req.session_id)
    if not session:
        return SessionStartResponse(session_id="", current_dimension="", question="会话不存在或已过期")

    dims = session["dimensions"]
    idx = session["current_index"]

    # 记录用户回答
    dims[idx]["value"] = req.answer
    dims[idx]["status"] = "filled"

    # 找下一个未填的必填维度
    next_idx = idx + 1
    while next_idx < len(dims) and dims[next_idx]["status"] == "filled":
        next_idx += 1

    if next_idx >= len(dims):
        # 所有必填维度已完成 → 生成结构化摘要
        summary = _build_summary(dims)
        session["finished"] = True
        return SessionStartResponse(
            session_id=req.session_id,
            current_dimension="",
            question=f"需求梳理完成。以下是结构化摘要：\n\n{summary}"
        )

    # 追问下一个维度
    dims[next_idx]["status"] = "active"
    session["current_index"] = next_idx
    d = dims[next_idx]
    # 用 AI 生成自然的追问
    prompt = f"用户刚回答了「{dims[idx]['name']}」: {req.answer}\n现在需要追问「{d['name']}」。请用自然友好的语气提一个问题（一句话即可）。"
    try:
        llm = LlmClient(settings.llm_api_key, settings.llm_base_url, settings.llm_model)
        resp = await llm.call(LlmRequest(user_message=prompt, max_tokens=100, temperature=0.7))
        question = resp.content.strip()
    except:
        question = f"了解了，接下来想了解一下您的{d['name']}情况。"
    return SessionStartResponse(session_id=req.session_id, current_dimension=d["name"], question=question)

@router.get("/session/{session_id}", response_model=SessionState)
async def get_session_state(session_id: str):
    """获取会话当前状态（断点恢复）"""
    session = _sessions.get(session_id)
    if not session:
        return SessionState(session_id="", dimensions=[], current_index=-1, finished=True)

    dims = session["dimensions"]
    idx = session["current_index"]
    return SessionState(
        session_id=session_id,
        dimensions=[{"name": d["name"], "value": d.get("value"), "status": d["status"]} for d in dims],
        current_index=idx,
        finished=idx >= len(dims)
    )

def _build_summary(dims: list[dict]) -> str:
    lines = []
    for d in dims:
        if d.get("value"):
            lines.append(f"· {d['name']}: {d['value']}")
    return "\n".join(lines)
