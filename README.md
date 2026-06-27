# SuperMap iObjects Java Maven 快速接入指南

本文用于把本地 SuperMap iObjects Java SDK 接入 Maven 项目。

适用环境：

```text
Windows 10 / 11
JDK 1.8
Maven 3.x
SuperMap iObjects Java SDK
```

本项目作业要求使用 Java + SuperMap iObjects Java，并实现 GUI、工作空间、数据源、数据集、地图、图层、查询等功能。

## 1. 准备 SDK

假设 SuperMap SDK 解压在：

```text
D:\supermap\supermap-iobjectsjava-2025u1-win-all
```

管理员运行一次：

```text
D:\supermap\supermap-iobjectsjava-2025u1-win-all\Install_x64.bat
```

然后验证官方示例是否能打开：

```bat
cd /d D:\supermap\supermap-iobjectsjava-2025u1-win-all\SampleCode
java -jar Startup.jar
```

能打开 GUI，说明 SDK、License、运行环境基本可用。

## 2. 导入 SuperMap jar 到 Maven 本地仓库

在项目根目录创建：

```text
scripts/install-supermap-jars.ps1
```

复制以下内容：

```powershell
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
```

执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install-supermap-jars.ps1
```

验证：

```powershell
mvn dependency:get "-Dartifact=com.supermap.local:com.supermap.data:2025u1"
```

没有报错即可。

## 3. pom.xml 添加依赖

在你的 Maven 项目 `pom.xml` 中加入：

```xml
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <supermap.version>2025u1</supermap.version>
</properties>

<dependencies>
    <dependency>
        <groupId>com.supermap.local</groupId>
        <artifactId>com.supermap.data</artifactId>
        <version>${supermap.version}</version>
    </dependency>

    <dependency>
        <groupId>com.supermap.local</groupId>
        <artifactId>com.supermap.mapping</artifactId>
        <version>${supermap.version}</version>
    </dependency>

    <dependency>
        <groupId>com.supermap.local</groupId>
        <artifactId>com.supermap.ui.controls</artifactId>
        <version>${supermap.version}</version>
    </dependency>

    <dependency>
        <groupId>com.supermap.local</groupId>
        <artifactId>com.supermap.data.cloudlicense</artifactId>
        <version>${supermap.version}</version>
    </dependency>
</dependencies>
```

如果后续提示缺少其他 SuperMap 包，就按 jar 文件名继续加依赖。

例如：

```text
com.supermap.analyst.spatialanalyst.jar
```

对应：

```xml
<dependency>
    <groupId>com.supermap.local</groupId>
    <artifactId>com.supermap.analyst.spatialanalyst</artifactId>
    <version>${supermap.version}</version>
</dependency>
```

## 4. 最小验证代码

创建：

```text
src/main/java/com/example/supermap/Main.java
```

写入：

```java
package com.example.supermap;

import com.supermap.data.Workspace;

public class Main {
    public static void main(String[] args) {
        Workspace workspace = new Workspace();
        System.out.println("SuperMap Workspace 创建成功：" + workspace);
    }
}
```

执行：

```bat
mvn clean compile
```

编译成功说明 Maven 依赖已接入。

## 5. IDEA 运行配置

如果运行时报 native library 错误，在 IDEA 中配置：

```text
Run → Edit Configurations → VM options
```

加入：

```text
-Djava.library.path=D:\supermap\supermap-iobjectsjava-2025u1-win-all\Bin
```

请从本机 SuperMap iObjects Java SDK 的 SampleData 目录复制演示数据到此目录；本仓库不提交官方示例数据。

