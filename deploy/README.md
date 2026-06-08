# AI 招投标系统 — 部署运维手册

## 快速部署

```bash
cp .env.example .env
# 编辑 .env 填入真实的 DEEPSEEK_API_KEY
docker-compose up -d
```

## 服务清单

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80 | 统一入口，前端+API代理 |
| FastAPI | 8000 | 后端 API |
| MySQL | 3306 | 业务数据库 |
| Milvus | 19530 | 向量数据库 |
| OnlyOffice | 3000 | 文档编辑器 |

## 健康检查

```bash
curl http://localhost/api/v1/chat -X POST -H 'Content-Type: application/json' -d '{"message":"test"}'
```

## 数据库备份

```bash
docker exec mysql mysqldump -u root -p aibidding > backup_$(date +%Y%m%d).sql
```

## 日志查看

```bash
docker-compose logs -f backend
docker-compose logs -f nginx
```

## 启停顺序

启动: milvus(etcd+minio) → mysql → backend → editor → nginx
停止: docker-compose down
重启: docker-compose restart backend
