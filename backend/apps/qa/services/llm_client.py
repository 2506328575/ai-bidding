"""LLM API 客户端 (httpx 异步调用)"""
import httpx
import time
from pydantic import BaseModel

class LlmRequest(BaseModel):
    system_prompt: str = ""
    user_message: str
    temperature: float = 0.7
    max_tokens: int = 2000

class LlmResponse(BaseModel):
    content: str
    tokens_used: int = 0
    model: str = ""
    latency_ms: int = 0

class LlmClient:
    def __init__(self, api_key: str, base_url: str, model: str,
                 connect_timeout: int = 5, read_timeout: int = 60):
        self.api_key = api_key
        self.base_url = base_url
        self.model = model
        self.timeout = httpx.Timeout(connect_timeout, read=read_timeout)

    async def call(self, req: LlmRequest) -> LlmResponse:
        start = time.time()
        messages = []
        if req.system_prompt:
            messages.append({"role": "system", "content": req.system_prompt})
        messages.append({"role": "user", "content": req.user_message})

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            resp = await client.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": messages,
                    "temperature": req.temperature,
                    "max_tokens": req.max_tokens,
                },
            )
            if resp.status_code == 401:
                raise Exception(f"LLM API Key 无效 (401)")
            if resp.status_code != 200:
                raise Exception(f"LLM API 返回错误: {resp.status_code} {resp.text[:200]}")

            data = resp.json()
            choice = data["choices"][0]
            return LlmResponse(
                content=choice["message"]["content"],
                tokens_used=data.get("usage", {}).get("total_tokens", 0),
                model=data.get("model", self.model),
                latency_ms=int((time.time() - start) * 1000),
            )
