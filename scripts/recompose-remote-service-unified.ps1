<#
.SYNOPSIS
    兼容 Windows/Unix 的 recompose 脚本：自动识别 `make` 或 `mingw32-make`。
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Service,
    [string]$Context = 'ecs',
    [string]$Project = 'academicsharingplatform',
    [string]$EnvFile = '.env'
)

Set-StrictMode -Version Latest

function Exec($cmd) {
    Write-Host "> $cmd" -ForegroundColor Cyan
    Invoke-Expression $cmd | Out-Host
    return $LASTEXITCODE
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "docker CLI not found. Please install docker."
    exit 2
}

$contexts = docker context ls --format '{{.Name}}' 2>$null
if (-not ($contexts -match "^$Context$")) {
    Write-Error "Docker context '$Context' not found."
    exit 3
}

try {
    Push-Location -Path (Get-Location)

    $stopCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml stop $Service"
    Exec $stopCmd | Out-Null

    $rmCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml rm -f $Service"
    Exec $rmCmd | Out-Null

    if (Test-Path ./Makefile) {
        $makeCmdName = if (Get-Command make -ErrorAction SilentlyContinue) { 'make' } elseif (Get-Command mingw32-make -ErrorAction SilentlyContinue) { 'mingw32-make' } else { $null }
        if (-not $makeCmdName) {
            Write-Warning "Makefile found but neither 'make' nor 'mingw32-make' is available in PATH; skipping local build."
        } else {
            $makeCmd = "$makeCmdName SERVICE=$Service build-service"
            $rc = Exec $makeCmd
            if ($rc -ne 0) { Write-Error "Local build failed (exit $rc)."; exit $rc }
        }
    } else {
        Write-Warning "Makefile not found; skipping local build."
    }

    # 如果 db compose 文件声明了 redis，则一并启动 redis，避免远端环境缺少依赖
    $composeFiles = "-f docker-compose.yml -f docker-compose-db.yml"

    # 检查本地 compose 是否声明 redis
    $declareRedis = $false
    if (Test-Path ./docker-compose-db.yml) {
        try {
            $content = Get-Content -Raw -Path ./docker-compose-db.yml -ErrorAction Stop
            if ($content -match "^\s*redis:\s*") { $declareRedis = $true }
        } catch {
            Write-Warning "无法读取 docker-compose-db.yml：$_"
        }
    }

    $includeRedis = $false
    if ($declareRedis) {
        # 先尝试检测远端同一 project 下的 redis 服务容器 id
        try {
            $remoteRedisId = & docker --context $Context compose -p $Project -f docker-compose.yml -f docker-compose-db.yml ps -q redis 2>$null
        } catch {
            $remoteRedisId = $null
        }

        $redisRunning = $false
        if ($remoteRedisId) {
            try {
                $state = & docker --context $Context inspect -f '{{.State.Running}}' $remoteRedisId 2>$null
                if ($state -match 'true') { $redisRunning = $true }
            } catch {
                $redisRunning = $false
            }
        } else {
            # 如果不是同一 project 的 redis，检测远端是否存在任何运行中的 redis 镜像容器
            try {
                $anyRedis = & docker --context $Context ps --filter ancestor=redis --format '{{.ID}}' 2>$null
                if ($anyRedis) { $redisRunning = $true }
            } catch {
                $redisRunning = $false
            }
        }

        if ($redisRunning) {
            Write-Host "检测到远端已运行 Redis，部署时将复用远端实例（不启动本地 compose 中的 redis）。" -ForegroundColor Yellow
            $includeRedis = $false
        } else {
            Write-Host "远端未检测到运行的 Redis，部署时会一并启动 compose 中的 redis 服务。" -ForegroundColor Cyan
            $includeRedis = $true
        }
    }

    $serviceList = @()
    if ($declareRedis -and $includeRedis) { $serviceList += 'redis' }
    $serviceList += $Service

    $upCmd = "docker --context $Context compose --env-file $EnvFile -p $Project $composeFiles up -d --build " + ($serviceList -join ' ')
    $rc = Exec $upCmd
    if ($rc -ne 0) { Write-Error "Remote compose up failed (exit $rc)."; exit $rc }

    Write-Host "Service '$Service' restarted on context '$Context'." -ForegroundColor Green
} finally {
    Pop-Location -ErrorAction SilentlyContinue
}
