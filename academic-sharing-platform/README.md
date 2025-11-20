# Academic Sharing Platform — Microservices Scaffold

This scaffold contains minimal microservice skeletons and a `docker-compose.yml` to run them locally for development.

Quick start (Windows `cmd.exe`):

```
docker compose up --build
```

Services created:
- `user-service` (port 3001)
- `achievement-service` (port 3002)
- `search-service` (placeholder)
- `data-sync-service` (placeholder)
- `file-service` (port 3005)
- `analytics-service` (placeholder)
- `system-service` (placeholder)

Datastores:
- MySQL (3306)
- MongoDB (27017)
- MinIO (9000)

Expand each service under `services/`.
