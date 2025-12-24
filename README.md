# AcademicSharingPlatform — Remote deploy with Docker Context

This README explains how to run `docker-compose-db.yml` directly on a remote ECS host from your local machine using Docker Context (no manual image push required). It also includes example `.env` and `.dockerignore` snippets.

Prerequisites
- Local machine: `docker` CLI with `docker context` and `docker compose` support.
- Remote ECS host: SSH access (user@ECS_IP), Docker Engine installed and running. The SSH user should be able to run Docker (in `docker` group or via sudo).

Quick overview (what happens)
- `docker context` will connect to the remote Docker daemon over SSH. When you run `docker --context <ctx> compose up`, the CLI will send the compose file and (if present) build context to the remote host; the remote daemon will build/pull images and start containers.

1) Create a Docker context for the ECS host
```powershell
docker context create ecs --docker "host=ssh://root@ECS_IP"
docker context ls
```

2) Run compose on ECS (remote build/pull and start)
```powershell
docker --context ecs compose --env-file .env -f docker-compose.yml -f docker-compose-db.yml up -d
docker --context ecs compose --env-file .env -f docker-compose.yml -f docker-compose-db.yml ps
docker --context ecs inspect <container>
docker --context ecs logs <container>
```

3) Recompose
```powershell
docker --context ecs compose --env-file .env -f docker-compose.yml -f docker-compose-db.yml stop
docker --context ecs compose --env-file .env -f docker-compose.yml -f docker-compose-db.yml rm -f
docker --context ecs volume ls -q | ForEach-Object { docker --context ecs volume rm $_ -f }
make
docker --context ecs compose --env-file .env -f docker-compose.yml -f docker-compose-db.yml up --build -d
```