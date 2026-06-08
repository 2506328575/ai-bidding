"""全局配置 — 通过 pydantic-settings 读取环境变量"""
from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    app_name: str = "AI 招投标系统"
    debug: bool = False

    # LLM
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com/v1"
    llm_model: str = "deepseek-chat"
    llm_connect_timeout: int = 5
    llm_read_timeout: int = 60
    llm_max_retries: int = 1

    # MySQL
    mysql_url: str = "sqlite:///aibidding.db"  # 开发用 SQLite

    # Milvus
    milvus_host: str = "localhost"
    milvus_port: int = 19530

    # CORS
    cors_origins: list[str] = ["http://localhost:5173", "http://localhost:3000"]

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}

@lru_cache()
def get_settings() -> Settings:
    return Settings()
