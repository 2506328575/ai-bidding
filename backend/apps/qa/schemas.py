"""M1 技术问答 — Pydantic Schema"""
from pydantic import BaseModel
from typing import Optional

class ChatRequest(BaseModel):
    message: str

class QaRequest(BaseModel):
    question: str

class Source(BaseModel):
    title: str
    relevance: str

class QaResponse(BaseModel):
    answer: str
    intent: str
    sources: list[Source] = []
    tokens_used: int = 0
    latency_ms: int = 0

class ChatResponse(BaseModel):
    type: str  # text | qa_result
    text: Optional[str] = None
    data: Optional[dict] = None
