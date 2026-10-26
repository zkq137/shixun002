<#
在本机搭一个 3 节点 Nacos 集群（共用同一个 MySQL 的 nacos 库）。

适用：Nacos 2.2+（cluster.conf 支持 IP:端口，所以同一台机器可以用不同端口模拟 3 个节点）。

用法：
    powershell -ExecutionPolicy Bypass -File scripts\setup-local-nacos-cluster.ps1

默认：
    源目录  D:\Develop\Nacos\nacos-server-2.4.0.1\nacos
    目标    D:\Develop\Nacos\cluster-local\node1 / node2 / node3
    端口    8848 / 8858 / 8868
    MySQL   127.0.0.1:3306/nacos（root/20050312）

可选参数：
    -Root <目录>            集群节点存放目录
    -Source <目录>          现有 Nacos 安装目录
    -Ports 8848,8858,8868   三个节点端口
    -MysqlUrl <jdbc url>    共享 MySQL 地址
    -MysqlUser / -MysqlPassword
    -Jdk8 <目录>            Nacos 用 JDK 8 启动
    -Recreate               删除并重建节点目录（会清掉这三个节点的本地数据/日志）
    -NoStart                只生成目录和配置，不启动

停止：scripts\stop-all.ps1 会杀掉所有 nacos 进程（包括这 3 个节点）。
#>
param(
    [string]$Root = 'D:\Develop\Nacos\cluster-local',
    [string]$Source = 'D:\Develop\Nacos\nacos-server-2.4.0.1\nacos',
    [int[]]$Ports = @(8848, 8858, 8868),
    [string]$MysqlUrl = 'jdbc:mysql://127.0.0.1:3306/nacos?characterEncoding=utf8&connectTimeout=1000&socketTimeout=3000&autoReconnect=true&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true',
    [string]$MysqlUser = 'root',
    [string]$MysqlPassword = '20050312',
    [string]$Jdk8 = 'D:\Develop\Java8',
    [switch]$Recreate,
    [switch]$NoStart
)

$ErrorActionPreference = 'Stop'

if ($Ports.Count -ne 3) {
    Write-Host '必须提供 3 个端口。' -ForegroundColor Red
    exit 1
}
if (-not (Test-Path (Join-Path $Source 'bin\startup.cmd'))) {
    Write-Host "找不到 Nacos 安装目录：$Source" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path (Join-Path $Jdk8 'bin\java.exe'))) {
    Write-Host "找不到 JDK 8：$Jdk8" -ForegroundColor Red
    exit 1
}

Write-Host "=== 1/4 准备节点目录（$Root）===" -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $Root | Out-Null
$clusterConf = ($Ports | ForEach-Object { "127.0.0.1:$_" }) -join "`r`n"
Write-Host "cluster.conf 内容："
Write-Host $clusterConf

for ($i = 0; $i -lt 3; $i++) {
    $nodeDir = Join-Path $Root ("node" + ($i + 1))
    if (Test-Path $nodeDir) {
        if ($Recreate) {
            Write-Host "  重建 $nodeDir"
            Remove-Item -LiteralPath $nodeDir -Recurse -Force
        }
    }
    if (-not (Test-Path $nodeDir)) {
        Write-Host "  复制 Nacos -> $nodeDir（约 160MB，稍等）"
        New-Item -ItemType Directory -Force -Path $nodeDir | Out-Null
        $rc = Start-Process -FilePath 'robocopy.exe' `
            -ArgumentList $Source, $nodeDir, '/E', '/NFL', '/NDL', '/NJH', '/NJS', '/NP', '/R:1', '/W:1' `
            -Wait -PassThru -WindowStyle Hidden
        if ($rc.ExitCode -ge 8) {
            Write-Host "  robocopy 失败，退出码 $($rc.ExitCode)" -ForegroundColor Red
            exit 1
        }
    }
}

Write-Host "=== 2/4 写入端口、数据库、cluster.conf ===" -ForegroundColor Cyan
for ($i = 0; $i -lt 3; $i++) {
    $nodeDir = Join-Path $Root ("node" + ($i + 1))
    $conf = Join-Path $nodeDir 'conf'

    [System.IO.File]::WriteAllText(
        (Join-Path $conf 'cluster.conf'),
        $clusterConf + "`r`n",
        (New-Object System.Text.UTF8Encoding($false))
    )

    $appConf = Join-Path $conf 'application.properties'
    $lines = [System.IO.File]::ReadAllLines($appConf)
    $newLines = New-Object System.Collections.Generic.List[string]
    $seen = @{}
    foreach ($line in $lines) {
        if ($line -match '^server\.port=') {
            $newLines.Add("server.port=$($Ports[$i])"); $seen['port'] = $true; continue
        }
        if ($line -match '^spring\.sql\.init\.platform=') {
            $newLines.Add('spring.sql.init.platform=mysql'); $seen['platform'] = $true; continue
        }
        if ($line -match '^db\.url\.0=') {
            $newLines.Add("db.url.0=$MysqlUrl"); $seen['url'] = $true; continue
        }
        if ($line -match '^db\.user\.0=') {
            $newLines.Add("db.user.0=$MysqlUser"); $seen['user'] = $true; continue
        }
        if ($line -match '^db\.password\.0=') {
            $newLines.Add("db.password.0=$MysqlPassword"); $seen['pwd'] = $true; continue
        }
        if ($line -match '^nacos\.core\.auth\.enabled=') {
            $newLines.Add('nacos.core.auth.enabled=false'); $seen['auth'] = $true; continue
        }
        if ($line -match '^nacos\.inetutils\.ip-address=') {
            # 本机多网卡（VMware/Radmin/VPN）时，Nacos 会选错 IP，这里强制用回环地址
            $newLines.Add('nacos.inetutils.ip-address=127.0.0.1'); $seen['inet'] = $true; continue
        }
        $newLines.Add($line)
    }
    if (-not $seen['port']) { $newLines.Add("server.port=$($Ports[$i])") }
    if (-not $seen['platform']) { $newLines.Add('spring.sql.init.platform=mysql') }
    if (-not $seen['url']) { $newLines.Add("db.url.0=$MysqlUrl") }
    if (-not $seen['user']) { $newLines.Add("db.user.0=$MysqlUser") }
    if (-not $seen['pwd']) { $newLines.Add("db.password.0=$MysqlPassword") }
    if (-not $seen['auth']) { $newLines.Add('nacos.core.auth.enabled=false') }
    if (-not $seen['inet']) { $newLines.Add('nacos.inetutils.ip-address=127.0.0.1') }
    [System.IO.File]::WriteAllLines($appConf, $newLines, (New-Object System.Text.UTF8Encoding($false)))

    Write-Host ("  node" + ($i + 1) + " -> 端口 " + $Ports[$i])
}

if ($NoStart) {
    Write-Host '已按 -NoStart 只生成配置，未启动。' -ForegroundColor Yellow
    exit 0
}

Write-Host "=== 3/4 启动 3 个节点（JDK 8）===" -ForegroundColor Cyan
$env:JAVA_HOME = $Jdk8
$logDir = Join-Path $env:TEMP 'nacos-local-cluster'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

for ($i = 0; $i -lt 3; $i++) {
    $nodeDir = Join-Path $Root ("node" + ($i + 1))
    Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'startup.cmd -m cluster' `
        -WorkingDirectory (Join-Path $nodeDir 'bin') -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir ("node" + ($i + 1) + ".log")) `
        -RedirectStandardError (Join-Path $logDir ("node" + ($i + 1) + ".err")) | Out-Null
    Write-Host ("  已拉起 node" + ($i + 1) + "（端口 " + $Ports[$i] + "）")
}

Write-Host "=== 4/4 等待并验证集群 ===" -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(150)
foreach ($port in $Ports) {
    while ((Get-Date) -lt $deadline) {
        if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { break }
        Start-Sleep -Seconds 2
    }
}

$allUp = $true
foreach ($port in $Ports) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        Write-Host "  端口 $port 已监听" -ForegroundColor Green
    } else {
        Write-Host "  端口 $port 未起来，看日志：$logDir" -ForegroundColor Red
        $allUp = $false
    }
}
if (-not $allUp) { exit 1 }

Start-Sleep -Seconds 15
try {
    $nodes = Invoke-RestMethod -Uri "http://127.0.0.1:$($Ports[0])/nacos/v1/core/cluster/nodes" -TimeoutSec 15
    Write-Host '  集群节点：'
    $nodes | ForEach-Object { Write-Host ("    " + $_.ip + ":" + $_.port + "  " + $_.state) }
} catch {
    Write-Host "  查询集群节点失败：$($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ''
Write-Host '集群已启动：' -ForegroundColor Cyan
$Ports | ForEach-Object { Write-Host "  http://127.0.0.1:$_/nacos" }
Write-Host ''
Write-Host '应用侧设置：'
Write-Host ('  $env:NACOS_SERVER_ADDR = "' + (($Ports | ForEach-Object { "127.0.0.1:$_" }) -join ',') + '"')
Write-Host '停止：powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1'
exit 0
