"""AI 招投标系统 — FastAPI 入口"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager

from core.settings import get_settings
from apps.qa.apis import router as qa_router
from apps.proposal.apis import router as proposal_router
from apps.bid.apis import router as bid_router
from apps.policy.apis import router as policy_router
from apps.requirement.apis import router as requirement_router

settings = get_settings()

@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期"""
    print(f"Starting {settings.app_name}...")
    yield
    print("Shutting down...")

app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(qa_router, prefix="/api/v1")
app.include_router(proposal_router, prefix="/api/v1")
app.include_router(bid_router, prefix="/api/v1")
app.include_router(policy_router, prefix="/api/v1")
app.include_router(requirement_router, prefix="/api/v1")

@app.get("/health")
async def health():
    return {"status": "ok", "app": settings.app_name}
