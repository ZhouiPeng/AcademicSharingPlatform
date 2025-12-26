<#
.SYNOPSIS
    在远程 Docker Context（默认为 ecs）上重新构建并重启指定服务的辅助脚本。

.DESCRIPTION
    脚本按 README 中（4）步骤封装了远程停止、移除、在本地构建并在远程以 compose 启动单个服务的流程。

.PARAMETER Service
    要重新构建并在远程启动的服务名（必需）。

.PARAMETER Context
    Docker context 名称，默认 "ecs"。

.PARAMETER Project
    Compose 项目名，默认 "academicsharingplatform"。

.PARAMETER EnvFile
    用于 compose 的 env 文件，默认 ".env"。

.EXAMPLE
    .\scripts\recompose-remote-service-fixed.ps1 -Service achievement-service
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Service,

    [Parameter(Position=1)]
    [string]$Context = 'ecs',

    [Parameter(Position=2)]
    [string]$Project = 'academicsharingplatform',

    [Parameter(Position=3)]
    [string]$EnvFile = '.env'
)

Set-StrictMode -Version Latest

function Exec($cmd) {
    Write-Host "> $cmd" -ForegroundColor Cyan
    Invoke-Expression $cmd | Out-Host
    return $LASTEXITCODE
}

try {
    docker --version > $null 2>&1
} catch {
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
        # Prefer 'make' (common on Unix / CI), fallback to 'mingw32-make' on Windows
        $makeCmdName = if (Get-Command make -ErrorAction SilentlyContinue) { 'make' } \
                       elseif (Get-Command mingw32-make -ErrorAction SilentlyContinue) { 'mingw32-make' } \
                       else { $null }

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

    $upCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml up -d --build $Service"
    $rc = Exec $upCmd
    if ($rc -ne 0) { Write-Error "Remote compose up failed (exit $rc)."; exit $rc }

    Write-Host "Service '$Service' restarted on context '$Context'." -ForegroundColor Green
} finally {
    Pop-Location -ErrorAction SilentlyContinue
}
<#
.SYNOPSIS
    在远程 Docker Context（默认为 ecs）上重新构建并重启指定服务的辅助脚本。

.DESCRIPTION
    脚本按 README 中（4）步骤封装了远程停止、移除、在本地构建并在远程以 compose 启动单个服务的流程。

.PARAMETER Service
    要重新构建并在远程启动的服务名（必需）。

.PARAMETER Context
    Docker context 名称，默认 "ecs"。

.PARAMETER Project
    Compose 项目名，默认 "academicsharingplatform"。

.PARAMETER EnvFile
    用于 compose 的 env 文件，默认 ".env"。
.EXAMPLE
    .\scripts\recompose-remote-service-fixed.ps1 -Service achievement-service
#>
<#
.SYNOPSIS
    Rebuild and restart a single service on a remote Docker context.

.DESCRIPTION
    This script stops and removes the target service on the remote context,
    optionally builds the service locally (via `make SERVICE=<service> build-service`),
    then runs `docker compose up --build` for that service on the remote context.

.PARAMETER Service
    Service name to recompose (required).

.PARAMETER Context
    Docker context name (default: ecs).

.PARAMETER Project
    Compose project name (default: academicsharingplatform).

.PARAMETER EnvFile
    Path to env file (default: .env).

.EXAMPLE
    .\scripts\recompose-remote-service-fixed.ps1 -Service achievement-service
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Service,

    [Parameter(Position=1)]
    [string]$Context = 'ecs',

    [Parameter(Position=2)]
    [string]$Project = 'academicsharingplatform',

    [Parameter(Position=3)]
    [string]$EnvFile = '.env'
)

Set-StrictMode -Version Latest

function Exec($cmd) {
    Write-Host "> $cmd" -ForegroundColor Cyan
    Invoke-Expression $cmd
    return $LASTEXITCODE
}

try {
    docker --version > $null 2>&1
} catch {
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

        $makeCmdName = if (Get-Command make -ErrorAction SilentlyContinue) { 'make' } \
                       elseif (Get-Command mingw32-make -ErrorAction SilentlyContinue) { 'mingw32-make' } \
                       else { $null }
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

    $upCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml up -d --build $Service"
    $rc = Exec $upCmd
    if ($rc -ne 0) { Write-Error "Remote compose up failed (exit $rc)."; exit $rc }

    Write-Host "Service '$Service' restarted on context '$Context'." -ForegroundColor Green
} finally {
    Pop-Location -ErrorAction SilentlyContinue
}
