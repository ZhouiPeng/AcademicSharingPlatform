<#
.SYNOPSIS
    统一的远程部署脚本：停止并删除指定服务，执行本地构建（可选），然后在远端 docker context 上 compose up 指定服务。失败时自动输出 compose config 和 mysql/服务日志。
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string] $Service,
    [string] $Context = 'ecs',
    [string] $Project = 'academicsharingplatform',
    [string] $EnvFile = '.env'
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

try {
    Push-Location -Path (Get-Location)

    # 读取 .env 并设置到进程环境（仅对需要的变量覆盖），部署后恢复
    $envVars = @{}
    if (Test-Path $EnvFile) {
        Write-Host ("读取 {0} 文件以强制使用远程配置" -f $EnvFile) -ForegroundColor Cyan
        foreach ($line in Get-Content -Path $EnvFile) {
            $t = $line.Trim()
            if ($t -and -not $t.StartsWith('#')) {
                if ($t -match '^([^=]+)=(.*)$') {
                    $key = $matches[1].Trim()
                    $value = $matches[2].Trim().Trim('"')
                    $envVars[$key] = $value
                }
            }
        }
    }

    $envBackup = @{}
    $envVarsToOverride = @(
        'MYSQL_HOST','MYSQL_PORT','MYSQL_USER','MYSQL_PASSWORD','MYSQL_USER_DB_NAME',
        'MYSQL_ACHIEVE_DB_NAME','MYSQL_FILE_DB_NAME','MYSQL_ADMIN_DB_NAME','MYSQL_ANALYTICS_DB_NAME',
        'MONGO_HOST','MONGO_PORT','MONGO_INITDB_ROOT_USERNAME','MONGO_INITDB_ROOT_PASSWORD',
        'MONGO_USER','MONGO_PASSWORD','MONGO_ADMIN_DB_NAME',
        'REDIS_HOST','REDIS_PORT','JWT_SECRET','SMTP_HOST','SMTP_PORT','SMTP_USER','SMTP_PASS',
        'OBS_ENDPOINT','OBS_ACCESS_KEY','OBS_SECRET_KEY','OBS_BUCKET'
    )

    foreach ($var in $envVarsToOverride) {
        if (Test-Path "Env:\$var") {
            $envBackup[$var] = (Get-Item "Env:\$var").Value
        }
        if ($envVars.ContainsKey($var)) {
            Set-Item -Path "Env:\$var" -Value $envVars[$var]
        }
    }

    try {
        # 停止并删除目标服务容器
        $stopCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml stop $Service"
        Exec $stopCmd | Out-Null

        $rmCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml rm -f $Service"
        Exec $rmCmd | Out-Null

        # 本地构建（可选）
        if (Test-Path ./Makefile) {
            $makeCmdName = $null
            if (Get-Command make -ErrorAction SilentlyContinue) { $makeCmdName = 'make' }
            elseif (Get-Command mingw32-make -ErrorAction SilentlyContinue) { $makeCmdName = 'mingw32-make' }

            if ($makeCmdName) {
                $makeCmd = "$makeCmdName SERVICE=$Service build-service"
                $rcBuild = Exec $makeCmd
                if ($rcBuild -ne 0) { Write-Error ("Local build failed (exit {0})." -f $rcBuild); exit $rcBuild }
            } else {
                Write-Warning "Makefile found but neither 'make' nor 'mingw32-make' is available in PATH; skipping local build."
            }
        } else {
            Write-Warning "Makefile not found; skipping local build."
        }

        # 判断是否需要包含 redis
        $composeFiles = "-f docker-compose.yml -f docker-compose-db.yml"
        $declareRedis = $false
        if (Test-Path ./docker-compose-db.yml) {
            try {
                $dbYaml = Get-Content -Raw -Path ./docker-compose-db.yml -ErrorAction Stop
                if ($dbYaml -match "^\s*redis:\s*") { $declareRedis = $true }
            } catch {
                Write-Warning ("无法读取 docker-compose-db.yml：{0}" -f $_)
            }
        }

        $includeRedis = $false
        if ($declareRedis) {
            try {
                $remoteRedisId = & docker --context $Context compose -p $Project -f docker-compose.yml -f docker-compose-db.yml ps -q redis 2>$null
            } catch { $remoteRedisId = $null }

            $redisRunning = $false
            if ($remoteRedisId) {
                try {
                    $state = & docker --context $Context inspect -f '{{.State.Running}}' $remoteRedisId 2>$null
                    if ($state -match 'true') { $redisRunning = $true }
                } catch { $redisRunning = $false }
            } else {
                try {
                    $anyRedis = & docker --context $Context ps --filter ancestor=redis --format '{{.ID}}' 2>$null
                    if ($anyRedis) { $redisRunning = $true }
                } catch { $redisRunning = $false }
            }

            if ($redisRunning) {
                Write-Host "检测到远端已运行 Redis，部署时将复用远端实例（不启动本地 compose 中的 redis）。" -ForegroundColor Yellow
            } else {
                Write-Host "远端未检测到运行的 Redis，部署时会一并启动 compose 中的 redis 服务。" -ForegroundColor Cyan
                $includeRedis = $true
            }
        }

        $serviceList = @()
        if ($declareRedis -and $includeRedis) { $serviceList += 'redis' }
        $serviceList += $Service

        # 远端部署（注意服务名在命令末尾，不跟在 --pull 后面）
        $upCmd = "docker --context $Context compose --env-file $EnvFile -p $Project $composeFiles up -d --build " + ($serviceList -join ' ')
        $rcUp = Exec $upCmd
        if ($rcUp -ne 0) {
            Write-Error ("Remote compose up failed (exit {0})." -f $rcUp)

            # 诊断信息
            Write-Host "`n=== docker context ls ===" -ForegroundColor Yellow
            docker context ls | Out-Host

            Write-Host "`n=== docker compose config (merged) ===" -ForegroundColor Yellow
            try {
                & docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml config | Out-Host
            } catch { Write-Warning ("compose config 输出失败：{0}" -f $_) }

            Write-Host "`n=== logs: mysql ===" -ForegroundColor Yellow
            try {
                & docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml logs --no-log-prefix --tail=200 mysql | Out-Host
            } catch { Write-Warning ("获取 mysql 日志失败：{0}" -f $_) }

            Write-Host ("`n=== logs: {0} ===" -f $Service) -ForegroundColor Yellow
            try {
                & docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml logs --no-log-prefix --tail=300 $Service | Out-Host
            } catch { Write-Warning ("获取 {0} 日志失败：{1}" -f $Service, $_) }

            exit $rcUp
        }

        Write-Host ("Service '{0}' restarted on context '{1}'." -f $Service, $Context) -ForegroundColor Green
    }
    finally {
        # 恢复环境变量
        foreach ($var in $envVarsToOverride) {
            if ($envBackup.ContainsKey($var)) {
                Set-Item -Path "Env:\$var" -Value $envBackup[$var]
            } elseif (Test-Path "Env:\$var") {
                Remove-Item "Env:\$var" -ErrorAction SilentlyContinue
            }
        }
        Write-Host "已恢复本地环境变量" -ForegroundColor Green
    }
}
finally {
    Pop-Location -ErrorAction SilentlyContinue
}
