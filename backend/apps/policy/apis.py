"""M5 政策打通 — API (骨架)"""
from fastapi import APIRouter

router = APIRouter(prefix="/policy", tags=["政策打通"])

@router.get("/list")
async def list_policies():
    return {"items": [], "total": 0, "message": "政策库功能开发中"}
