"""M5 政策打通 — API"""
from fastapi import APIRouter, Depends, Query
from pydantic import BaseModel
from datetime import date, datetime
from typing import Optional
from core.settings import get_settings

router = APIRouter(prefix="/policy", tags=["政策打通"])

# 内存存储 (Demo)
_policies: list[dict] = []

class PolicyCreate(BaseModel):
    province: str
    city: Optional[str] = None
    category: str  # 社保/公积金/积分落户
    effective_date: date
    expire_date: Optional[date] = None
    content: str
    source_url: Optional[str] = None

class PolicyResponse(BaseModel):
    id: str
    province: str
    city: Optional[str] = None
    category: str
    effective_date: str
    expire_date: Optional[str] = None
    content: str
    source_url: Optional[str] = None
    source_type: str = "MANUAL"
    status: str = "PUBLISHED"
    created_at: str

import uuid, json

@router.get("/list")
async def list_policies(
    province: Optional[str] = Query(None),
    city: Optional[str] = Query(None),
    category: Optional[str] = Query(None),
):
    """政策列表查询 — 支持省/市/类别过滤 + 三级级联降级"""
    results = _policies
    if province:
        results = [p for p in results if p["province"] == province]
        # 市级精确匹配优先，再降级到省级
        if city:
            city_match = [p for p in results if p.get("city") == city]
            if city_match:
                results = city_match
    if category:
        results = [p for p in results if p["category"] == category]
    # 只返回已发布且未过期的
    today = date.today().isoformat()
    results = [p for p in results
               if p["status"] == "PUBLISHED"
               and (not p.get("expire_date") or p["expire_date"] >= today)]
    return {"items": results, "total": len(results)}

@router.post("/create")
async def create_policy(req: PolicyCreate):
    """录入政策 (手动录入或AI抓取后人工确认)"""
    p = {
        "id": uuid.uuid4().hex[:8],
        "province": req.province,
        "city": req.city,
        "category": req.category,
        "effective_date": req.effective_date.isoformat(),
        "expire_date": req.expire_date.isoformat() if req.expire_date else None,
        "content": req.content,
        "source_url": req.source_url,
        "source_type": "MANUAL",
        "status": "PUBLISHED",
        "created_at": datetime.now().isoformat(),
    }
    _policies.append(p)
    return p

@router.post("/patrol")
async def patrol():
    """定时巡检 — 检查过期政策并自动标记"""
    today = date.today().isoformat()
    expired_count = 0
    for p in _policies:
        if p.get("expire_date") and p["expire_date"] < today and p["status"] == "PUBLISHED":
            p["status"] = "EXPIRED"
            expired_count += 1
    return {"patrolled": len(_policies), "expired_marked": expired_count}

@router.post("/{policy_id}/expire")
async def expire_policy(policy_id: str):
    """手动标记政策过期"""
    for p in _policies:
        if p["id"] == policy_id:
            p["status"] = "EXPIRED"
            return p
    return {"error": "政策不存在"}
