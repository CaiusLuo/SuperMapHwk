param(
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

if (!(Test-Path $BackupFile)) {
    Write-Host "没有找到备份文件: $BackupFile" -ForegroundColor Red
    Write-Host "无法自动恢复电脑名。"
    exit 1
}

$originalName = (Get-Content $BackupFile -Raw).Trim()
$currentName = $env:COMPUTERNAME

Write-Host "当前电脑名: $currentName"
Write-Host "准备恢复为: $originalName"

if ($currentName -eq $originalName) {
    Write-Host "当前电脑名已经是原名称，无需恢复。" -ForegroundColor Green
    exit 0
}

Rename-Computer -NewName $originalName -Force

Write-Host "电脑名已恢复，需要重启后生效。" -ForegroundColor Yellow

if ($NoRestart) {
    Write-Host "你传入了 -NoRestart，请稍后手动重启。"
}
else {
    Write-Host "5 秒后自动重启..."
    Start-Sleep -Seconds 5
    Restart-Computer -Force
}
