# AcademicSharingPlatform — Remote deploy with Docker Context

This README explains how to run `docker-compose-db.yml` directly on a remote ECS host from your local machine using Docker Context (no manual image push required). It also includes example `.env` and `.dockerignore` snippets.

Prerequisites
- Local machine: `docker` CLI with `docker context` and `docker compose` support.
- Remote ECS host: SSH access (user@ECS_IP), Docker Engine installed and running. The SSH user should be able to run Docker (in `docker` group or via sudo).

Quick overview (what happens)
- `docker context` will connect to the remote Docker daemon over SSH. When you run `docker --context <ctx> compose up`, the CLI will send the compose file and (if present) build context to the remote host; the remote daemon will build/pull images and start containers.

1) Create a Docker context for the ECS host
```powershell
# 在本地创建名为 `ecs` 的 Docker 上下文，远程 Docker 守护通过 SSH 连接到 root@ECS_IP
docker context create ecs --docker "host=ssh://root@ECS_IP"
# 列出本机上的所有 Docker 上下文，确认刚创建的上下文存在
docker context ls
```

2) Run compose on ECS (remote build/pull and start)
```powershell
# 使用名为 `ecs` 的上下文在远程主机上以后台模式启动（构建/拉取镜像并创建容器）
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml up -d
# 列出远程主机上由 compose 启动的容器状态
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml ps
# 查看指定容器的详细信息
docker --context ecs inspect <container>
# 查看指定容器的日志输出
docker --context ecs logs <container>
```

3) Recompose
```powershell
# 停止远程主机上由 compose 管理的容器（不删除）
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml stop
# 强制移除远程主机上由 compose 创建的容器
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml rm -f
# 列出远程主机上的所有卷并逐个删除（谨慎操作，会丢失持久数据）
docker --context ecs volume ls -q | ForEach-Object { docker --context ecs volume rm $_ -f }
# 在本地运行项目的 Makefile（通常用于构建/准备镜像或生成资源）
make
# 重新构建镜像并在远程主机上以后台模式启动容器
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml up --build -d
```

4) Recompose a specific service
```powershell
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml stop <service>
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml rm -f <service>
docker --context ecs volume rm <volume> -f
make SERVICE=<service> build-service
docker --context ecs compose --env-file .env -p academicsharingplatform -f docker-compose.yml -f docker-compose-db.yml up -d --build <service>
```