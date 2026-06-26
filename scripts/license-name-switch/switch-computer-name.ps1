param(
    [Parameter(Mandatory = $true)]
    [string]$TargetName,

    [string]$BackupFile = "$PSScriptRoot\original-computer-name.txt",

    [switch]$NoRestart
)

function Assert-Admin {
    $currentUser = [Security.Principal.WindowsIdentity]::GetCurrent()
    $principal = New-Object Security.Principal.WindowsPrincipal($currentUser)

    if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
        Write-Host "请使用管理员权限运行 PowerShell。" -ForegroundColor Red
        exit 1
    }
}

Assert-Admin

$currentName = $env:COMPUTERNAME

Write-Host "当前电脑名: $currentName"
Write-Host "目标电脑名: $TargetName"

if ($TargetName.Length -gt 15) {
    Write-Host "Windows 电脑名建议不超过 15 个字符，请换一个更短的名称。" -ForegroundColor Red
    exit 1
}

if (!(Test-Path $BackupFile)) {
    Set-Content -Path $BackupFile -Value $currentName -Encoding UTF8
    Write-Host "已备份原电脑名到: $BackupFile"
}
else {
    Write-Host "检测到已存在备份文件: $BackupFile"
    Write-Host "不会覆盖原备份。"
}

if ($currentName -eq $TargetName) {
    Write-Host "当前电脑名已经是 $TargetName，无需修改。" -ForegroundColor Green
    exit 0
}

Write-Host "正在修改电脑名为: $TargetName"

Rename-Computer -NewName $TargetName -Force

Write-Host "电脑名已修改，需要重启后生效。" -ForegroundColor Yellow

if ($NoRestart) {
    Write-Host "你传入了 -NoRestart，请稍后手动重启。"
}
else {
    Write-Host "5 秒后自动重启..."
    Start-Sleep -Seconds 5
    Restart-Computer -Force
}
