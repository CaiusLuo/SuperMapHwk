# 修改成你的 SuperMap SDK 路径
$superMapHome = "D:\supermap\supermap-iobjectsjava-2025u1-win-all"

# Maven 坐标版本，可按 SDK 版本自行修改
$version = "2025u1"
$groupId = "com.supermap.local"

$jarDir = Join-Path $superMapHome "Bin"
$tempPomDir = "D:\supermap\maven-poms"

if (!(Test-Path $jarDir)) {
    Write-Host "SuperMap Bin directory not found: $jarDir" -ForegroundColor Red
    exit 1
}

if (!(Test-Path $tempPomDir)) {
    New-Item -ItemType Directory -Path $tempPomDir | Out-Null
}

$localRepo = (mvn help:evaluate "-Dexpression=settings.localRepository" -q "-DforceStdout").Trim()
$supermapRepo = Join-Path $localRepo "com\supermap\local"

Write-Host "Maven local repository: $localRepo"
Write-Host "SuperMap jar directory: $jarDir"

# 清理旧的 SuperMap 本地依赖，避免旧 POM 污染
if (Test-Path $supermapRepo) {
    Write-Host "Deleting old SuperMap local artifacts: $supermapRepo"
    Remove-Item $supermapRepo -Recurse -Force
}

Get-ChildItem $jarDir -Filter "*.jar" | ForEach-Object {
    $artifactId = [System.IO.Path]::GetFileNameWithoutExtension($_.Name)
    $pomPath = Join-Path $tempPomDir "$artifactId.pom"

    $pomContent = @"
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$groupId</groupId>
    <artifactId>$artifactId</artifactId>
    <version>$version</version>
    <packaging>jar</packaging>
</project>
"@

    Set-Content -Path $pomPath -Value $pomContent -Encoding UTF8

    Write-Host "Installing $($_.Name) as ${groupId}:${artifactId}:${version}"

    mvn install:install-file `
        "-Dfile=$($_.FullName)" `
        "-DpomFile=$pomPath"
}

Write-Host "SuperMap jars installed successfully." -ForegroundColor Green
