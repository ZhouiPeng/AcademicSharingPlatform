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
    .\scripts\recompose-remote-service.ps1 -Service achievement-service
#>
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
    .\scripts\recompose-remote-service.ps1 -Service achievement-service
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
    # 检查 docker 可用性
    docker --version > $null 2>&1
} catch {
    Write-Error "docker CLI 未找到，请先安装并配置 docker。"
    exit 2
}

# 检查 context 是否存在
$contexts = docker context ls --format '{{.Name}}' 2>$null
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
    .\scripts\recompose-remote-service.ps1 -Service achievement-service
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
    # 检查 docker 可用性
    docker --version > $null 2>&1
} catch {
    Write-Error "docker CLI 未找到，请先安装并配置 docker。"
    exit 2
}

# 检查 context 是否存在
$contexts = docker context ls --format '{{.Name}}' 2>$null
if (-not ($contexts -match "^$Context$")) {
    Write-Error "Docker context '$Context' 不存在。请运行 'docker context create' 或检查 context 名称。"
    exit 3
}

try {
    Push-Location -Path (Get-Location)
    # 在远端停止并移除目标服务容器（由 compose 管理）
    $stopCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml stop $Service"
    Exec $stopCmd | Out-Null

    $rmCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml rm -f $Service"
    Exec $rmCmd | Out-Null

    # 在本地构建服务镜像（依赖仓库中的 Makefile）
    if (Test-Path ./Makefile) {
        $makeCmd = "make SERVICE=$Service build-service"
        $rc = Exec $makeCmd
        if ($rc -ne 0) { Write-Error "本地 make 构建失败（exit $rc）。中止."; exit $rc }
    } else {
        Write-Warning "未发现 Makefile，跳过本地构建步骤。若需要请手动构建镜像。"
    }

    # 在远程以 compose 启动并构建目标服务
    $upCmd = "docker --context $Context compose --env-file $EnvFile -p $Project -f docker-compose.yml -f docker-compose-db.yml up -d --build $Service"
    $rc = Exec $upCmd
    if ($rc -ne 0) { Write-Error "远程 compose 启动失败（exit $rc）。"; exit $rc }

    Write-Host "服务 '$Service' 已在远程 context '$Context' 成功重启。" -ForegroundColor Green
} finally {
    Pop-Location -ErrorAction SilentlyContinue
}
