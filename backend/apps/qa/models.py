"""M1 技术问答 — 数据模型 (SQLModel)"""
from sqlmodel import SQLModel, Field
from typing import Optional
from datetime import datetime

class ChatSession(SQLModel, table=True):
    __tablename__ = "chat_session"
    id: str = Field(primary_key=True)
    title: Optional[str] = None
    status: str = "active"

class ChatMessage(SQLModel, table=True):
    __tablename__ = "chat_message"
    id: Optional[int] = Field(default=None, primary_key=True)
    session_id: str = Field(foreign_key="chat_session.id", index=True)
    role: str  # user / assistant
    content: str
    intent: Optional[str] = None
    tokens_used: int = 0
    latency_ms: int = 0
    created_at: datetime = Field(default_factory=datetime.utcnow)
