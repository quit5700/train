$ErrorActionPreference = 'Stop'

$source = Join-Path $PSScriptRoot 'dist\fabric-train-simulator-0.2.14-mc26.2-fabric.jar'
$mods = 'G:\game\mcfabric\.minecraft\versions\26.2\mods'
$target = Join-Path $mods 'fabric-train-simulator-0.2.14-mc26.2-fabric.jar'

if (-not (Test-Path -LiteralPath $source)) {
    throw "找不到 0.2.14 JAR：$source"
}
if (-not (Test-Path -LiteralPath $mods)) {
    throw "找不到 Minecraft 26.2 的 mods 目录：$mods"
}
if (Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @('java.exe', 'javaw.exe') -and $_.CommandLine -like '*G:\game\mcfabric\.minecraft\versions\26.2*'
    }) {
    throw 'Minecraft 26.2 正在运行，请正常退出游戏后再安装。'
}

$active = @(Get-ChildItem -LiteralPath $mods -File | Where-Object {
        $_.Name -like 'fabric-train-simulator-*.jar' -and $_.Name -ne (Split-Path -Leaf $target)
    })
foreach ($file in $active) {
    $version = if ($file.BaseName -match 'fabric-train-simulator-(.+)-mc26\.2-fabric') { $Matches[1] } else { 'unknown' }
    $backup = Join-Path $mods "_fabric_train_backup\$version"
    New-Item -ItemType Directory -Path $backup -Force | Out-Null
    Move-Item -LiteralPath $file.FullName -Destination (Join-Path $backup $file.Name) -Force
}

Copy-Item -LiteralPath $source -Destination $target -Force
$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
if ($sourceHash -ne $targetHash) {
    throw "安装后 SHA-256（安全哈希算法）不一致：源=$sourceHash，目标=$targetHash"
}

Write-Host "安装成功：$target"
Write-Host "SHA-256：$targetHash"
