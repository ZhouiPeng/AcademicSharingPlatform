<#
.SYNOPSIS
    远程服务部署脚本 - 用于在远程Docker Context上重新部署指定服务

.DESCRIPTION
    此脚本会执行以下操作：
    1. 停止并删除远程指定服务容器
    2. 在本地构建服务（使用Maven）
    3. 在远程Docker context上重新构建并启动服务

.PARAMETER Service
    要部署的服务名称（必需），例如：user-service, gateway-service 等

.PARAMETER Context
    Docker context 名称（可选，默认为 'ecs'）

.PARAMETER Project
    Docker Compose 项目名称（可选，默认为 'academicsharingplatform'）

.PARAMETER SkipBuild
    跳过本地Maven构建（可选）

.EXAMPLE
    .\scripts\deploy-service.ps1 -Service user-service
    
.EXAMPLE
    .\scripts\deploy-service.ps1 -Service gateway-service -SkipBuild

.EXAMPLE
    .\scripts\deploy-service.ps1 -Service admin-service -Context ecs -Project myproject
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0, HelpMessage="服务名称，例如：user-service")]
    [ValidateNotNullOrEmpty()]
    [string]$Service,

    [Parameter(HelpMessage="Docker context 名称")]
    [string]$Context = 'ecs',

    [Parameter(HelpMessage="Docker Compose 项目名称")]
    [string]$Project = 'academicsharingplatform',

    [Parameter(HelpMessage="跳过本地Maven构建")]
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# 定义颜色输出函数
function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Error-Custom {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Warning-Custom {
    param([string]$Message)
    Write-Host "[WARNING] $Message" -ForegroundColor Yellow
}

# 检查Docker是否安装
function Test-Docker {
    try {
        $null = docker --version
        return $true
    }
    catch {
        Write-Error-Custom "Docker CLI 未找到，请先安装 Docker"
        return $false
    }
}

# 检查Docker Context是否存在
function Test-DockerContext {
    param([string]$ContextName)
    
    $contexts = docker context ls --format '{{.Name}}' 2>$null
    if ($contexts -contains $ContextName) {
        return $true
    }
    else {
        Write-Error-Custom "Docker context '$ContextName' 不存在"
        Write-Info "可用的 contexts："
        docker context ls
        return $false
    }
}

# 执行Docker Compose命令
function Invoke-DockerCompose {
    param(
        [string]$Command,
        [string]$Description
    )
    
    Write-Info $Description
    Write-Host "> $Command" -ForegroundColor DarkGray
    
    $result = Invoke-Expression $Command 2>&1
    $exitCode = $LASTEXITCODE
    
    if ($result) {
        Write-Host $result
    }
    
    if ($exitCode -ne 0) {
        Write-Error-Custom "命令执行失败 (退出码: $exitCode)"
        throw "Docker Compose 命令失败"
    }
    
    return $result
}

# 主执行流程
try {
    Write-Host "`n========================================" -ForegroundColor Magenta
    Write-Host "  远程服务部署脚本" -ForegroundColor Magenta
    Write-Host "========================================`n" -ForegroundColor Magenta
    
    Write-Info "服务名称: $Service"
    Write-Info "Docker Context: $Context"
    Write-Info "项目名称: $Project"
    Write-Host ""

    # 1. 检查前置条件
    Write-Info "检查前置条件..."
    if (-not (Test-Docker)) {
        exit 1
    }
    
    if (-not (Test-DockerContext -ContextName $Context)) {
        exit 1
    }
    
    # 2. 停止远程服务
    Write-Host "`n[步骤 1/4] 停止远程服务容器" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    try {
        $stopCmd = "docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml stop $Service"
        Invoke-DockerCompose -Command $stopCmd -Description "正在停止 $Service..."
    }
    catch {
        Write-Warning-Custom "停止服务失败，可能服务未运行，继续执行..."
    }
    
    # 3. 删除远程容器
    Write-Host "`n[步骤 2/4] 删除远程容器" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    try {
        $rmCmd = "docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml rm -f $Service"
        Invoke-DockerCompose -Command $rmCmd -Description "正在删除 $Service 容器..."
    }
    catch {
        Write-Warning-Custom "删除容器失败，可能容器不存在，继续执行..."
    }
    
    # 4. 本地构建（如果未跳过）
    Write-Host "`n[步骤 3/4] 本地构建服务" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    
    if ($SkipBuild) {
        Write-Warning-Custom "跳过本地构建（使用 -SkipBuild 参数）"
    }
    else {
        $servicePath = "services\$Service"
        
        if (-not (Test-Path $servicePath)) {
            Write-Error-Custom "服务目录不存在: $servicePath"
            exit 1
        }
        
        Write-Info "切换到服务目录: $servicePath"
        Push-Location $servicePath
        
        try {
            Write-Info "执行 Maven 构建..."
            Write-Host "> mvn clean package -DskipTests" -ForegroundColor DarkGray
            
            $mvnOutput = mvn clean package -DskipTests 2>&1
            $mvnExitCode = $LASTEXITCODE
            
            # 只显示关键信息
            $mvnOutput | Where-Object {
                $_ -match '\[INFO\] BUILD' -or 
                $_ -match '\[ERROR\]' -or 
                $_ -match '\[WARNING\]' -or
                $_ -match 'Building jar'
            } | ForEach-Object {
                if ($_ -match '\[ERROR\]') {
                    Write-Host $_ -ForegroundColor Red
                }
                elseif ($_ -match '\[WARNING\]') {
                    Write-Host $_ -ForegroundColor Yellow
                }
                else {
                    Write-Host $_
                }
            }
            
            if ($mvnExitCode -ne 0) {
                Write-Error-Custom "Maven 构建失败"
                exit $mvnExitCode
            }
            
            Write-Success "本地构建完成"
        }
        finally {
            Pop-Location
        }
    }
    
    # 5. 远程构建并启动
    Write-Host "`n[步骤 4/4] 远程构建并启动服务" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    
    $upCmd = "docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml up -d --build $Service"
    Invoke-DockerCompose -Command $upCmd -Description "正在远程构建并启动 $Service..."
    
    # 6. 等待并检查状态
    Write-Host "`n检查服务状态..." -ForegroundColor Yellow
    Write-Info "等待 5 秒让服务启动..."
    Start-Sleep -Seconds 5
    
    $psCmd = "docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml ps $Service"
    Write-Host "> $psCmd" -ForegroundColor DarkGray
    $status = Invoke-Expression $psCmd
    Write-Host $status
    
    # 7. 显示日志
    Write-Host "`n最新日志（最后 20 行）：" -ForegroundColor Yellow
    Write-Host "----------------------------------------" -ForegroundColor Yellow
    $logsCmd = "docker --context $Context compose --env-file .env -p $Project -f docker-compose.yml -f docker-compose-db.yml logs $Service --tail 20"
    Write-Host "> $logsCmd" -ForegroundColor DarkGray
    Invoke-Expression $logsCmd
    
    # 完成
    Write-Host "`n========================================" -ForegroundColor Magenta
    Write-Success "服务 '$Service' 已成功部署到 '$Context'"
    Write-Host "========================================`n" -ForegroundColor Magenta
    
    Write-Info "提示：可以使用以下命令查看实时日志："
    Write-Host "  docker --context $Context logs -f academicsharingplatform-$Service-1" -ForegroundColor Gray
    
    exit 0
}
catch {
    Write-Host "`n========================================" -ForegroundColor Red
    Write-Error-Custom "部署失败: $_"
    Write-Host "========================================`n" -ForegroundColor Red
    
    Write-Info "调试建议："
    Write-Host "  1. 检查服务名称是否正确" -ForegroundColor Gray
    Write-Host "  2. 检查 Docker context 连接: docker --context $Context ps" -ForegroundColor Gray
    Write-Host "  3. 检查 .env 文件是否存在且配置正确" -ForegroundColor Gray
    Write-Host "  4. 查看详细日志: docker --context $Context logs academicsharingplatform-$Service-1" -ForegroundColor Gray
    
    exit 1
}
