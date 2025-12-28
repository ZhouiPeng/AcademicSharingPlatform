<#
.SYNOPSIS
    远程服务部署脚本

.DESCRIPTION
    用于在远程Docker Context上重新部署指定服务

.PARAMETER Service
    服务名称（必需）

.PARAMETER Context
    Docker context名称（默认'ecs'）

.PARAMETER SkipBuild
    跳过本地构建
    
.EXAMPLE
    .\deploy-simple.ps1 user-service
    .\deploy-simple.ps1 user-service -SkipBuild
#>

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

param(
    [Parameter(Mandatory=$true)]
    [string]$Service,
    
    [string]$Context = 'ecs',
    [string]$Project = 'academicsharingplatform',
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Continue'

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "  部署服务: $Service" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# 1. 停止服务
Write-Host "[1/4] 停止服务..." -ForegroundColor Yellow
docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml stop $Service

# 2. 删除容器
Write-Host "`n[2/4] 删除容器..." -ForegroundColor Yellow
docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml rm -f $Service

# 3. 本地构建
if (-not $SkipBuild) {
    Write-Host "`n[3/4] 本地构建..." -ForegroundColor Yellow
    Push-Location "services\$Service"
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "构建失败" -ForegroundColor Red
        Pop-Location
        exit 1
    }
    Pop-Location
    Write-Host "构建完成" -ForegroundColor Green
} else {
    Write-Host "`n[3/4] 跳过本地构建" -ForegroundColor Yellow
}

# 4. 远程部署
Write-Host "`n[4/4] 远程构建并启动..." -ForegroundColor Yellow
docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml up -d --build $Service

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n等待服务启动..." -ForegroundColor Yellow
    Start-Sleep -Seconds 5
    
    Write-Host "`n服务状态:" -ForegroundColor Yellow
    docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml ps $Service
    
    Write-Host "`n最新日志:" -ForegroundColor Yellow
    docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml logs $Service --tail 15
    
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "  部署成功！" -ForegroundColor Green
    Write-Host "========================================`n" -ForegroundColor Green
} else {
    Write-Host "`n部署失败！" -ForegroundColor Red
    exit 1
}
