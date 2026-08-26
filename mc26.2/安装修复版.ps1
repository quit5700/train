$ErrorActionPreference = 'Stop'

$source = Join-Path $PSScriptRoot 'dist\fabric-train-simulator-0.1.0-mc26.2-fabric.jar'
$target = 'G:\game\mcfabric\.minecraft\versions\26.2\mods\fabric-train-simulator-0.1.0-mc26.2-fabric.jar'

if (-not (Test-Path -LiteralPath $source)) {
    throw "找不到修复版 JAR：$source"
}
if (-not (Test-Path -LiteralPath (Split-Path -Parent $target))) {
    throw "找不到 Minecraft 26.2 的 mods 目录：$(Split-Path -Parent $target)"
}

Copy-Item -LiteralPath $source -Destination $target -Force

$sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
$targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
if ($sourceHash -ne $targetHash) {
    throw "安装后 SHA-256（安全哈希算法）不一致：源=$sourceHash，目标=$targetHash"
}

Write-Host "安装成功：$target"
Write-Host "SHA-256：$targetHash"
