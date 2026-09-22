<#
停止本项目启动的所有进程：网关 + 5 个业务服务 + Sentinel 控制台 + Nacos。

用法（在项目根目录执行）：
    powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1

只杀命令行里带 talent-* 模块名、sentinel-dashboard、nacos 的 java/mvn 进程，
不会碰你机器上别的 Java 程序。
#>
param(
    [switch]$KeepNacos,
    [switch]$KeepSentinel
)

$ErrorActionPreference = 'Stop'

function Get-TargetProcesses {
    Get-CimInstance Win32_Process |
        Where-Object {
            $_.CommandLine -and (
                $_.CommandLine -match 'talent-(gateway|employee|planning|training|promotion|analysis)-service|talent-gateway' -or
                ($_.CommandLine -match 'spring-boot:run' -and $_.CommandLine -match 'talent-')
            )
        }
}

$killed = @()

$targets = Get-TargetProcesses
foreach ($p in ($targets | Where-Object { $_.Name -eq 'cmd.exe' })) {
    & taskkill.exe /PID $p.ProcessId /T /F 2>&1 | Out-Null
    $killed += "服务 mvn (PID $($p.ProcessId))"
}
# mvn 被 taskkill /T 带走后，残留的 java 子进程也清一下
Start-Sleep -Seconds 3
foreach ($p in (Get-TargetProcesses | Where-Object { $_.Name -eq 'java.exe' })) {
    & taskkill.exe /PID $p.ProcessId /T /F 2>&1 | Out-Null
    $killed += "服务 java (PID $($p.ProcessId))"
}

if (-not $KeepSentinel) {
    foreach ($p in (Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
                    Where-Object { $_.CommandLine -match 'sentinel-dashboard' })) {
        & taskkill.exe /PID $p.ProcessId /T /F 2>&1 | Out-Null
        $killed += "Sentinel 控制台 (PID $($p.ProcessId))"
    }
}

if (-not $KeepNacos) {
    foreach ($p in (Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
                    Where-Object { $_.CommandLine -match 'nacos' })) {
        & taskkill.exe /PID $p.ProcessId /T /F 2>&1 | Out-Null
        $killed += "Nacos (PID $($p.ProcessId))"
    }
}

Start-Sleep -Seconds 5

if ($killed.Count -eq 0) {
    Write-Host '没有找到本项目的进程（可能已经停了）'
} else {
    Write-Host "已停止 $($killed.Count) 个进程：" -ForegroundColor Cyan
    $killed | ForEach-Object { Write-Host "  $_" }
}

$still = @()
foreach ($port in @(9090, 8081, 8082, 8083, 8084, 8085, 8858, 8848)) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) {
        $still += $port
    }
}
if ($still.Count -gt 0) {
    Write-Host "以下端口还在被占用：$($still -join ', ')（可能是别人也在用）" -ForegroundColor Yellow
} else {
    Write-Host '端口全部已释放' -ForegroundColor Green
}

exit 0
