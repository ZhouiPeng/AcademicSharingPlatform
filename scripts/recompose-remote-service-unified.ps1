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

# 前置检查
if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Error "docker CLI not found. Please install docker."
    exit 2
}

$contexts = docker context ls --format '{{.Name}}' 2>$null
if (-not ($contexts -match "^$Context$")) {
    Write-Error "Docker context '$Context' not found."
    exit 3
}

# 尝试获取 context 详细信息并检查是否为 ssh 类型，若是则做额外的连通性/认证诊断
try {
    $ctxRaw = & docker context inspect $Context --format '{{json .}}' 2>$null
    if ($ctxRaw) {
        try {
            $ctx = $ctxRaw | ConvertFrom-Json
            if ($ctx.Endpoints -and $ctx.Endpoints.ssh) {
                $sshHost = $ctx.Endpoints.ssh.Host
                $sshUser = $ctx.Endpoints.ssh.User
                Write-Host "Detected docker context '$Context' using SSH endpoint: $sshUser@$sshHost" -ForegroundColor Yellow
                # 检查网络连通性（PowerShell 环境下）
                try {
                    $tnc = Test-NetConnection -ComputerName $sshHost -Port 22 -WarningAction SilentlyContinue
                    if ($tnc.TcpTestSucceeded) {
                        Write-Host "TCP 22 to $sshHost OK" -ForegroundColor Green
                    } else {
                        Write-Warning "TCP 22 to $sshHost failed: $($tnc)"
                    }
                } catch {
                    Write-Warning "Test-NetConnection failed: $_"
                }

                # 如果本机有 ssh 客户端，尝试快速认证测试（BatchMode 避免交互式密码）
                if (Get-Command ssh -ErrorAction SilentlyContinue) {
                    try {
                        Write-Host "Attempting non-interactive SSH test to $sshUser@$sshHost..." -ForegroundColor Cyan
                        $sshTest = & ssh -o BatchMode=yes -o ConnectTimeout=10 ($sshUser + "@" + $sshHost) echo OK 2>&1
                        Write-Host "SSH test output: $sshTest"
                    } catch {
                        Write-Warning "SSH test failed: $_"
                    }
                } else {
                    Write-Warning "ssh client not found locally; skipping quick auth test."
                }
            }
        } catch {
            Write-Warning "Failed to parse docker context inspect output: $_"
        }
    }
} catch {
    Write-Warning "Failed to inspect docker context '$Context': $_"
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
