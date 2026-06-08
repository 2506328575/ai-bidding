# DevOps 工程师 SOP

## 部署架构
```
┌──────────────────────────────────────┐
│  Nginx (:80)                          │
│  ├── /              → Vue3 前端静态   │
│  ├── /api/*         → FastAPI (:8000) │
│  └── /editor/*      → OnlyOffice(:3000)│
├──────────────────────────────────────┤
│  FastAPI (:8000)                       │
│  Milvus (:19530)                       │
│  MySQL (:3306)                         │
│  OnlyOffice Web (:3000)                │
└──────────────────────────────────────┘
```

## Docker Compose 服务

| 服务 | 镜像 | 端口 |
|------|------|------|
| nginx | nginx:alpine | 80 |
| backend | python:3.11-slim | 8000 |
| mysql | mysql:8.0 | 3306 |
| milvus | milvusdb/milvus:latest | 19530 |
| editor | node:20-alpine | 3000 |

## 环境变量 (.env)
- DEEPSEEK_API_KEY
- MYSQL_ROOT_PASSWORD
- MILVUS_HOST
- APP_ENV (development/production)

## CI/CD (GitHub Actions)
- ci.yml: lint + test + build
- deploy.yml: docker build + push + deploy

## 部署脚本 (deploy.sh)
1. 拉取最新代码
2. 构建 Docker 镜像
3. 启动 docker-compose
4. 运行数据库迁移
5. 健康检查

## 交付物清单
- [ ] docker-compose.yml
- [ ] Dockerfile.backend
- [ ] Dockerfile.frontend
- [ ] nginx.conf
- [ ] .env.example
- [ ] deploy.sh
- [ ] .github/workflows/ci.yml
- [ ] README.md (运维手册)
