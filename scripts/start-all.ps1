<#
一键启动整个平台：Nacos + Sentinel 控制台 + 网关 + 5 个业务服务。

用法（在项目根目录执行）：
    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1

参数都可以按自己机器改：
    -Jdk17      JDK 17 路径（编译运行微服务用）
    -Jdk8       JDK 8 路径（只用来启动 Nacos）
    -NacosHome  Nacos 安装目录（含 bin\startup.cmd）
    -Maven      mvn 可执行文件
    -Dashboard  Sentinel 控制台 jar，没有就跳过
    -SkipSentinel  不启动控制台

启动完会打印每个服务是否起来、以及几个可以直接点的调试地址。
停止全部：scripts\stop-all.ps1
#>
param(
    [string]$Jdk17 = 'D:\Develop\jdk17',
    [string]$Jdk8 = 'D:\Develop\Java8',
    [string]$NacosHome = 'D:\Develop\Nacos\nacos-server-2.4.0.1\nacos',
    [string]$Maven = 'D:\develop\apache-maven-3.6.1-bin\apache-maven-3.6.1\bin\mvn.cmd',
    [string]$Dashboard = 'D:\Shixun\sentinel-dashboard-1.8.8.jar',
    [int]$DashboardPort = 8858,
    [switch]$SkipSentinel,
    [int]$TimeoutSec = 120
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$logDir = Join-Path $env:TEMP 'talent-logs'
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$services = @(
    @{ Name = 'talent-gateway';          Port = 9090 },
    @{ Name = 'talent-employee-service'; Port = 8081 },
    @{ Name = 'talent-planning-service'; Port = 8082 },
    @{ Name = 'talent-training-service'; Port = 8083 },
    @{ Name = 'talent-promotion-service'; Port = 8084 },
    @{ Name = 'talent-analysis-service'; Port = 8085 }
)

function Test-Port([int]$port) {
    [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port([int]$port, [string]$what, [int]$timeout = 90) {
    for ($i = 0; $i -lt $timeout; $i++) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 1
    }
    Write-Host "  [超时] $what 的端口 $port 一直没起来，去看日志：$logDir" -ForegroundColor Yellow
    return $false
}

Write-Host '=== 1/4 检查环境 ===' -ForegroundColor Cyan

if (-not (Test-Path (Join-Path $Jdk17 'bin\java.exe'))) {
    Write-Host "找不到 JDK 17：$Jdk17（用 -Jdk17 指定自己的路径）" -ForegroundColor Red
    exit 1
}
if (-not (Test-Path $Maven)) {
    $found = Get-Command mvn -ErrorAction SilentlyContinue
    if ($found) { $Maven = $found.Source }
    else {
        Write-Host "找不到 mvn：$Maven（用 -Maven 指定）" -ForegroundColor Red
        exit 1
    }
}
$env:JAVA_HOME = $Jdk17
Write-Host "  JDK17 = $Jdk17"
Write-Host "  Maven = $Maven"
if (Test-Port 3306) { Write-Host '  MySQL 3306 正常' }
else { Write-Host '  [警告] MySQL 3306 没监听，服务起来后连库会失败' -ForegroundColor Yellow }

Write-Host '=== 2/4 启动 Nacos ===' -ForegroundColor Cyan
if (Test-Port 8848) {
    Write-Host '  Nacos 已经在运行，跳过'
} else {
    $startup = Join-Path $NacosHome 'bin\startup.cmd'
    if (-not (Test-Path $startup)) {
        Write-Host "  [警告] 找不到 Nacos：$startup（用 -NacosHome 指定），先跳过" -ForegroundColor Yellow
    } else {
        if (-not (Test-Path (Join-Path $Jdk8 'bin\java.exe'))) {
            Write-Host "  [警告] 找不到 JDK 8：$Jdk8（Nacos 2.4 要用 JDK 8 启动）" -ForegroundColor Yellow
        }
        $env:JAVA_HOME = $Jdk8
        Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'startup.cmd -m standalone' `
            -WorkingDirectory (Join-Path $NacosHome 'bin') -WindowStyle Hidden `
            -RedirectStandardOutput (Join-Path $logDir 'nacos.log') `
            -RedirectStandardError (Join-Path $logDir 'nacos.err') | Out-Null
        $env:JAVA_HOME = $Jdk17
        if (Wait-Port 8848 'Nacos' 60) { Write-Host '  Nacos 已启动：http://127.0.0.1:8848/nacos' }
    }
}

Write-Host '=== 3/4 启动 Sentinel 控制台 ===' -ForegroundColor Cyan
if ($SkipSentinel) {
    Write-Host '  按要求跳过（不影响业务，只是看不到监控）'
} elseif (Test-Port $DashboardPort) {
    Write-Host "  控制台已经在运行（$DashboardPort）"
} elseif (-not (Test-Path $Dashboard)) {
    Write-Host "  [警告] 找不到控制台 jar：$Dashboard，先跳过（可用 -Dashboard 指定）" -ForegroundColor Yellow
} else {
    Start-Process -FilePath (Join-Path $Jdk17 'bin\java.exe') `
        -ArgumentList "-Dserver.port=$DashboardPort", '-Dsentinel.dashboard.auth.username=sentinel',
                      '-Dsentinel.dashboard.auth.password=sentinel', '-jar', $Dashboard `
        -WindowStyle Hidden -RedirectStandardOutput (Join-Path $logDir 'sentinel-dashboard.log') `
        -RedirectStandardError (Join-Path $logDir 'sentinel-dashboard.err') | Out-Null
    if (Wait-Port $DashboardPort 'Sentinel 控制台' 60) {
        Write-Host "  控制台已启动：http://127.0.0.1:$DashboardPort （sentinel / sentinel）"
    }
}

Write-Host '=== 4/4 启动网关和 5 个业务服务 ===' -ForegroundColor Cyan
foreach ($s in $services) {
    if (Test-Port $s.Port) {
        Write-Host "  $($s.Name) 已经在跑（$($s.Port)），跳过"
        continue
    }
    Start-Process -FilePath $Maven -ArgumentList '-pl', $s.Name, 'spring-boot:run' `
        -WorkingDirectory $root -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $logDir "$($s.Name).log") `
        -RedirectStandardError (Join-Path $logDir "$($s.Name).err") | Out-Null
}

Write-Host ''
Write-Host '等待服务注册到 Nacos（第一次启动要 1 分钟左右）...' -ForegroundColor Cyan
$ok = 0
foreach ($s in $services) {
    if (Wait-Port $s.Port $s.Name $TimeoutSec) {
        Write-Host "  [OK]   $($s.Name.PadRight(26)) 端口 $($s.Port)" -ForegroundColor Green
        $ok++
    } else {
        Write-Host "  [FAIL] $($s.Name.PadRight(26)) 端口 $($s.Port)" -ForegroundColor Red
    }
}

Write-Host ''
Write-Host "启动完成：$ok / $($services.Count) 个服务在跑" -ForegroundColor Cyan
Write-Host '常用地址：'
Write-Host '  网关入口      http://127.0.0.1:9090/api/employee/page?pageNum=1&pageSize=5'
Write-Host '  人才看板      http://127.0.0.1:9090/api/analysis/dashboard'
Write-Host '  Nacos 控制台  http://127.0.0.1:8848/nacos'
if (-not $SkipSentinel) { Write-Host "  Sentinel      http://127.0.0.1:$DashboardPort" }
Write-Host "日志目录：$logDir"
Write-Host '停止全部：powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1'

# 显式退出：Start-Process 启动的子进程会继承句柄，不写这句的话在某些终端里脚本会"跑完了但不返回"
exit 0
