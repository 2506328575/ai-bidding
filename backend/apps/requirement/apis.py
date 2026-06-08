"""M2 需求梳理 — API (骨架)"""
from fastapi import APIRouter

router = APIRouter(prefix="/requirement", tags=["需求梳理"])

@router.get("/dimensions")
async def list_dimensions():
    return {"dimensions": [
        {"name": "行业", "type": "ENUM", "required": True},
        {"name": "规模", "type": "ENUM", "required": True},
        {"name": "核心痛点", "type": "TEXTAREA", "required": True},
        {"name": "预算范围", "type": "ENUM", "required": True},
        {"name": "交付时限", "type": "DATE", "required": True},
    ]}
