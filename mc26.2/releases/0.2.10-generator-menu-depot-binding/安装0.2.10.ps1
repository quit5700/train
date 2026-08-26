[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$sourceJar = Join-Path $PSScriptRoot 'dist\fabric-train-simulator-0.2.10-mc26.2-fabric.jar'
$modsDirectory = 'G:\game\mcfabric\.minecraft\versions\26.2\mods'
$targetJar = Join-Path $modsDirectory 'fabric-train-simulator-0.2.10-mc26.2-fabric.jar'
$backupDirectory = 'G:\game\mcfabric\.minecraft\versions\26.2\mods-backup\fabric-train-simulator'
$expectedHash = 'FBA53194F2025CAE375623527584E343085A8BF365D0E9C7CF6ED9B79DB307AC'

if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Cannot find the 0.2.10 mod JAR: $sourceJar"
}
if (-not (Test-Path -LiteralPath $modsDirectory -PathType Container)) {
    throw "Cannot find the Minecraft 26.2 mods directory: $modsDirectory"
}
if ((Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Source JAR SHA-256 validation failed. Installation cancelled.'
}

$activeJars = @(Get-ChildItem -LiteralPath $modsDirectory -Filter 'fabric-train-simulator-*.jar' -File)
foreach ($oldJar in $activeJars) {
    if ($oldJar.FullName -eq $targetJar) { continue }
    try {
        $lockProbe = [System.IO.File]::Open($oldJar.FullName, 'Open', 'Read', 'None')
        $lockProbe.Dispose()
    }
    catch {
        throw 'Minecraft is using the old mod. Save and exit the game, then run this script again.'
    }
}

$temporaryJar = "$targetJar.new"
Copy-Item -LiteralPath $sourceJar -Destination $temporaryJar -Force
if ((Get-FileHash -LiteralPath $temporaryJar -Algorithm SHA256).Hash -ne $expectedHash) {
    Remove-Item -LiteralPath $temporaryJar -Force
    throw 'Temporary JAR validation failed. The old mod was not moved.'
}

if (Test-Path -LiteralPath $targetJar -PathType Leaf) {
    if ((Get-FileHash -LiteralPath $targetJar -Algorithm SHA256).Hash -ne $expectedHash) {
        Remove-Item -LiteralPath $temporaryJar -Force
        throw 'A different 0.2.10 JAR already exists at the target. Refusing to overwrite it.'
    }
    Remove-Item -LiteralPath $temporaryJar -Force
}
else {
    Move-Item -LiteralPath $temporaryJar -Destination $targetJar
}

$oldJars = @($activeJars | Where-Object { $_.FullName -ne $targetJar })
if ($oldJars.Count -gt 0) {
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    foreach ($oldJar in $oldJars) {
        $backupName = '{0}-{1}.jar' -f $oldJar.BaseName, (Get-Date -Format 'yyyyMMdd-HHmmssfff')
        Move-Item -LiteralPath $oldJar.FullName -Destination (Join-Path $backupDirectory $backupName)
    }
}

$installedJars = @(Get-ChildItem -LiteralPath $modsDirectory -Filter 'fabric-train-simulator-*.jar' -File)
if ($installedJars.Count -ne 1 -or $installedJars[0].FullName -ne $targetJar) {
    throw 'The mods directory does not contain exactly one active train mod JAR.'
}
if ((Get-FileHash -LiteralPath $targetJar -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Final installed JAR hash does not match.'
}

Write-Host 'Fabric Train Simulator 0.2.10 installed and verified. No shutdown was scheduled.'
