# 自动部署脚本使用说明

## 脚本文件

- **deploy-simple.ps1** - 简化版部署脚本（推荐使用）

## 使用方法

### 1. 基本用法

```powershell
# 部署指定服务（包含本地构建）
.\scripts\deploy-simple.ps1 user-service

# 跳过本地构建，直接使用现有JAR部署
.\scripts\deploy-simple.ps1 user-service -SkipBuild

# 部署其他服务
.\scripts\deploy-simple.ps1 gateway-service
.\scripts\deploy-simple.ps1 admin-service
```

### 2. 参数说明

| 参数 | 说明 | 必需 | 默认值 |
|------|------|------|--------|
| `Service` | 服务名称 | 是 | - |
| `Context` | Docker context名称 | 否 | ecs |
| `Project` | Docker Compose项目名 | 否 | academicsharingplatform |
| `SkipBuild` | 跳过本地Maven构建 | 否 | false |

### 3. 部署流程

脚本会自动执行以下步骤：

1. **停止远程服务** - 停止ECS上运行的指定服务容器
2. **删除远程容器** - 清理旧的容器实例
3. **本地构建**（可选） - 使用Maven在本地构建服务JAR包
4. **远程部署** - 在ECS上重新构建Docker镜像并启动容器
5. **状态检查** - 显示服务状态和最新日志

### 4. 示例

#### 完整部署（包含构建）

```powershell
# 从源代码重新构建并部署user-service
.\scripts\deploy-simple.ps1 user-service
```

输出：
```
========================================
  部署服务: user-service
========================================

[1/4] 停止服务...
[2/4] 删除容器...
[3/4] 本地构建...
[INFO] BUILD SUCCESS
[4/4] 远程构建并启动...
[+] Running 2/2
 ✔ Container academicsharingplatform-mysql-1         Healthy
 ✔ Container academicsharingplatform-user-service-1  Started

服务状态:
NAME                                     STATUS
academicsharingplatform-user-service-1   Up 5 seconds (healthy)

========================================
  部署成功！
========================================
```

#### 快速部署（跳过构建）

```powershell
# 只重新部署，不重新构建JAR
.\scripts\deploy-simple.ps1 user-service -SkipBuild
```

适用场景：
- 只修改了配置文件（如application.yml、Dockerfile）
- JAR包已经是最新的
- 需要快速重启服务

### 5. 手动部署命令（不使用脚本）

如果脚本无法运行，可以使用以下手动命令：

```powershell
# 1. 停止服务
docker --context ecs compose --env-file .env -p academicsharingplatform `
    -f docker-compose.yml -f docker-compose-db.yml stop user-service

# 2. 删除容器
docker --context ecs compose --env-file .env -p academicsharingplatform `
    -f docker-compose.yml -f docker-compose-db.yml rm -f user-service

# 3. 本地构建
cd services/user-service
mvn clean package -DskipTests
cd ../..

# 4. 远程部署
docker --context ecs compose --env-file .env -p academicsharingplatform `
    -f docker-compose.yml -f docker-compose-db.yml up -d --build user-service

# 5. 查看状态
docker --context ecs compose --env-file .env -p academicsharingplatform `
    -f docker-compose.yml -f docker-compose-db.yml ps user-service

# 6. 查看日志
docker --context ecs compose --env-file .env -p academicsharingplatform `
    -f docker-compose.yml -f docker-compose-db.yml logs user-service --tail 20
```

### 6. 常见问题

#### Q: 脚本执行失败，提示"无法识别为cmdlet"
A: 需要设置执行策略：
```powershell
PowerShell -ExecutionPolicy Bypass -File .\scripts\deploy-simple.ps1 user-service
```

#### Q: Maven构建失败
A: 检查服务目录和pom.xml是否正确，或使用`-SkipBuild`跳过构建

#### Q: Docker context 'ecs' 不存在
A: 需要先创建Docker context：
```powershell
docker context create ecs --docker "host=ssh://root@ECS_IP"
```

#### Q: 如何查看实时日志？
A: 使用以下命令：
```powershell
docker --context ecs logs -f academicsharingplatform-user-service-1
```

### 7. 支持的服务列表

- user-service
- achievement-service
- admin-service
- analytics-service
- file-service
- gateway-service
- data-sync-service

### 8. 注意事项

1. 确保本地`.env`文件存在且配置正确
2. 确保有ECS服务器的SSH访问权限
3. 部署前建议先查看当前服务状态
4. 数据库服务（mysql、mongo、redis）不建议使用此脚本重启

---

**最后更新**: 2025-12-28
