"""M3 方案生成 — API"""
from fastapi import APIRouter, Depends
from pydantic import BaseModel
from apps.qa.services.llm_client import LlmClient, LlmRequest
from core.settings import get_settings
import re, time

router = APIRouter(prefix="/proposal", tags=["方案生成"])

class ProposalRequest(BaseModel):
    industry: str
    scale: str = ""
    pain_points: str = ""
    budget: str = ""

class Section(BaseModel):
    title: str
    key_point: str
    content: str = ""
    tokens_used: int = 0

class ProposalResult(BaseModel):
    title: str
    outline: str
    sections: list[Section]
    total_tokens: int = 0
    total_latency_ms: int = 0

@router.post("/generate", response_model=ProposalResult)
async def generate(req: ProposalRequest, settings=Depends(get_settings)):
    llm = LlmClient(settings.llm_api_key, settings.llm_base_url, settings.llm_model)
    start = time.time()

    # Step 1: 大纲生成
    outline_prompt = f"你是售前方案专家。根据客户需求生成6-8个章节大纲。\n行业:{req.industry}\n痛点:{req.pain_points}\n预算:{req.budget}\n\n输出格式每行: 1. 章节名: 要点"
    resp = await llm.call(LlmRequest(system_prompt="你是售前方案架构师。", user_message=outline_prompt, max_tokens=500, temperature=0.5))
    outline = resp.content
    sections = _parse_outline(outline)

    # Step 2: 逐节生成
    prev_summary = ""
    total_tokens = resp.tokens_used
    for sec in sections:
        prompt = (f"撰写【{sec.title}】章节。\n行业:{req.industry}\n痛点:{req.pain_points}\n"
                  f"要点:{sec.key_point}\n前文:{prev_summary}\n\n要求: 800-1200字，专业具体，直接写正文。")
        try:
            r = await llm.call(LlmRequest(system_prompt="你是售前方案专家。", user_message=prompt, max_tokens=2000, temperature=0.7))
            cleaned = _strip_preamble(r.content)
            sec.content = cleaned
            sec.tokens_used = r.tokens_used
            total_tokens += r.tokens_used
            prev_summary = cleaned[:200]
        except Exception as e:
            sec.content = f"(生成失败: {e})"

    return ProposalResult(
        title=f"{req.industry}售前技术方案",
        outline=outline, sections=sections,
        total_tokens=total_tokens,
        total_latency_ms=int((time.time()-start)*1000))

def _parse_outline(text: str) -> list[Section]:
    sections = []
    for line in text.split("\n"):
        line = line.strip()
        if re.match(r"^\d+[\.\、]", line):
            parts = re.split(r"[:：]", line, maxsplit=1)
            title = re.sub(r"^\d+[\.\、]\s*", "", parts[0])
            sections.append(Section(title=title, key_point=parts[1].strip() if len(parts)>1 else ""))
    if not sections:
        for t in ["项目概述","需求分析","方案设计","实施计划","服务保障"]:
            sections.append(Section(title=t, key_point=""))
    return sections

def _strip_preamble(text: str) -> str:
    return re.sub(r"^(好的[，,]\s*)?(作为.*?[,，]\s*)?(我将.*?[。\s]*)?", "", text).strip()
