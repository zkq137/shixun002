<#
把本机的 Nacos 以「集群模式」启动（每个节点都要执行一次）。

用法（三台机器分别执行，-Nodes 内容必须完全一致）：
    powershell -ExecutionPolicy Bypass -File scripts\start-nacos-cluster.ps1 `
        -Nodes "10.1.248.86","10.1.248.12","10.1.248.13"

只看生成的 cluster.conf、不真正改文件和启动：
    ... -DryRun

参数：
    -Nodes       集群所有节点的 IP（至少 3 个，且必须是不同的 IP）
    -NacosHome   Nacos 安装目录（含 bin\startup.cmd）
    -Jdk8        启动 Nacos 用的 JDK 8
    -ServerPort  节点端口，默认 8848（gRPC 会自动用 +1000/+1001，Raft 用 -1000）
    -DryRun      只预览

注意：
    1. 集群模式下所有节点必须连同一个 MySQL 的 nacos 库；
    2. Nacos 2.x 必须额外放行 8848 / 9848 / 9849 / 7848 四个 TCP 端口；
    3. 不要把同一台机器用不同端口伪装成多个节点——Raft 节点身份是按 IP 算的，
       必须是 3 个不同的 IP（可以是 3 台机器、3 个容器或云的 3 台实例）。
#>
param(
    [Parameter(Mandatory = $true)]
    [string[]]$Nodes,
    [string]$NacosHome = 'D:\Develop\Nacos\nacos-server-2.4.0.1\nacos',
    [string]$Jdk8 = 'D:\Develop\Java8',
    [int]$ServerPort = 8848,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

Write-Host '=== 1/4 检查参数 ===' -ForegroundColor Cyan
# 兼容两种写法：PowerShell 里传数组 -Nodes "a","b" / 命令行传一串 "a,b,c"
$Nodes = $Nodes | ForEach-Object { $_ -split '[,;\s]+' } |
    ForEach-Object { $_.Trim() } | Where-Object { $_ } | Select-Object -Unique
if ($Nodes.Count -lt 3) {
    Write-Host "集群至少需要 3 个不同的节点 IP，当前只有 $($Nodes.Count) 个。" -ForegroundColor Red
    exit 1
}
if ($Nodes.Count % 2 -eq 0) {
    Write-Host "警告：$($Nodes.Count) 个节点是偶数，Raft 建议用奇数（3 或 5）个。" -ForegroundColor Yellow
}
foreach ($node in $Nodes) {
    if ($node -notmatch '^\d{1,3}(\.\d{1,3}){3}$') {
        Write-Host "节点不是合法 IPv4：$node" -ForegroundColor Red
        exit 1
    }
}
Write-Host ('  节点：' + ($Nodes -join ', '))

$startup = Join-Path $NacosHome 'bin\startup.cmd'
$confDir = Join-Path $NacosHome 'conf'
if (-not (Test-Path $startup)) {
    Write-Host "找不到 Nacos 启动脚本：$startup（用 -NacosHome 指定）" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path (Join-Path $Jdk8 'bin\java.exe'))) {
    Write-Host "找不到 JDK 8：$Jdk8（用 -Jdk8 指定，Nacos 2.4 建议用 JDK 8 启动）" -ForegroundColor Red
    exit 1
}

$localIps = @(Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty IPAddress)
$isLocal = $false
foreach ($node in $Nodes) {
    if ($localIps -contains $node) {
        $isLocal = $true
    }
}
if (-not $isLocal) {
    Write-Host "警告：本机 IP（$($localIps -join ', ')）不在 -Nodes 里。Nacos 会报 current ip not found in cluster.conf。" -ForegroundColor Yellow
}

Write-Host '=== 2/4 检查数据库配置 ===' -ForegroundColor Cyan
$appConf = Join-Path $confDir 'application.properties'
if (Test-Path $appConf) {
    $dbLines = Get-Content $appConf | Where-Object {
        $_ -match '^\s*(spring\.sql\.init\.platform|db\.num|db\.url\.0|db\.user\.0)'
    }
    if ($dbLines) {
        $dbLines | ForEach-Object { Write-Host "  $_" }
    }
    if (-not ($dbLines -match 'spring\.sql\.init\.platform=mysql')) {
        Write-Host '  警告：没看到 spring.sql.init.platform=mysql，集群模式必须用外部 MySQL，不能是内嵌 Derby。' -ForegroundColor Yellow
    }
    if (-not ($dbLines -match 'db\.url\.0=')) {
        Write-Host '  警告：没看到 db.url.0，请先配好共享 MySQL 再启动。' -ForegroundColor Yellow
    }
} else {
    Write-Host "  找不到 $appConf" -ForegroundColor Yellow
}

Write-Host '=== 3/4 生成 cluster.conf ===' -ForegroundColor Cyan
$content = ($Nodes | ForEach-Object { "$_`:$ServerPort" }) -join "`r`n"
Write-Host $content
$clusterConf = Join-Path $confDir 'cluster.conf'

if ($DryRun) {
    Write-Host ''
    Write-Host '[DryRun] 不会写文件、不会启动 Nacos。' -ForegroundColor Yellow
    Write-Host "正式执行时会写入：$clusterConf"
    Write-Host "然后执行：`"$startup`" -m cluster"
    exit 0
}

if (Test-Path $clusterConf) {
    $backup = "$clusterConf.bak-$(Get-Date -Format 'yyyyMMddHHmmss')"
    Copy-Item -LiteralPath $clusterConf -Destination $backup -Force
    Write-Host "  已备份原 cluster.conf -> $backup"
}
[System.IO.File]::WriteAllText($clusterConf, $content + "`r`n", (New-Object System.Text.UTF8Encoding($false)))
Write-Host "  已写入 $clusterConf" -ForegroundColor Green

Write-Host '=== 4/4 启动集群节点 ===' -ForegroundColor Cyan
$env:JAVA_HOME = $Jdk8
$logDir = Join-Path $env:TEMP 'nacos-cluster-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$nodeName = 'nacos-' + ($localIps | Where-Object { $Nodes -contains $_ } | Select-Object -First 1)

Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'startup.cmd -m cluster' `
    -WorkingDirectory (Join-Path $NacosHome 'bin') -WindowStyle Hidden `
    -RedirectStandardOutput (Join-Path $logDir "$nodeName.log") `
    -RedirectStandardError (Join-Path $logDir "$nodeName.err") | Out-Null

$ok = $false
for ($i = 0; $i -lt 90; $i++) {
    if (Get-NetTCPConnection -LocalPort $ServerPort -State Listen -ErrorAction SilentlyContinue) {
        $ok = $true
        break
    }
    Start-Sleep -Seconds 1
}

if ($ok) {
    Write-Host "  节点已启动：http://$($localIps | Where-Object { $Nodes -contains $_ } | Select-Object -First 1):$ServerPort/nacos" -ForegroundColor Green
    Write-Host "  日志：$logDir" -ForegroundColor Gray
} else {
    Write-Host "  90 秒内没等到 $ServerPort 端口，去看日志：$logDir" -ForegroundColor Red
    exit 1
}

Write-Host ''
Write-Host '下一步：' -ForegroundColor Cyan
Write-Host '  1. 在另外两台机器上用完全相同的 -Nodes 再执行一次本脚本；'
Write-Host '  2. 控制台 → 集群管理 → 节点列表，确认所有节点都 UP；'
Write-Host '  3. 应用端把 NACOS_SERVER_ADDR 配成逗号分隔的三个地址。'
exit 0
