[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)

$sourceJar = Join-Path $PSScriptRoot 'dist\fabric-train-simulator-0.2.9-mc26.2-fabric.jar'
$modsDirectory = 'G:\game\mcfabric\.minecraft\versions\26.2\mods'
$targetJar = Join-Path $modsDirectory 'fabric-train-simulator-0.2.9-mc26.2-fabric.jar'
$previousJar = Join-Path $modsDirectory 'fabric-train-simulator-0.2.8-mc26.2-fabric.jar'
$backupDirectory = 'G:\game\mcfabric\.minecraft\versions\26.2\mods-backup\fabric-train-simulator'
$expectedHash = 'EB8B44B368D93DF392811EFD22881C145536E6D59C07D340ECFE5093DC79B063'

if (-not (Test-Path -LiteralPath $sourceJar -PathType Leaf)) {
    throw "Cannot find the 0.2.9 mod JAR: $sourceJar"
}
if (-not (Test-Path -LiteralPath $modsDirectory -PathType Container)) {
    throw "Cannot find the Minecraft 26.2 mods directory: $modsDirectory"
}
if ((Get-FileHash -LiteralPath $sourceJar -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Source JAR SHA-256 validation failed. Installation cancelled.'
}

if (Test-Path -LiteralPath $previousJar -PathType Leaf) {
    try {
        $lockProbe = [System.IO.File]::Open($previousJar, 'Open', 'Read', 'None')
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
        throw 'A different 0.2.9 JAR already exists at the target. Refusing to overwrite it.'
    }
    Remove-Item -LiteralPath $temporaryJar -Force
}
else {
    Move-Item -LiteralPath $temporaryJar -Destination $targetJar
}

if (Test-Path -LiteralPath $previousJar -PathType Leaf) {
    New-Item -ItemType Directory -Path $backupDirectory -Force | Out-Null
    $backupName = 'fabric-train-simulator-0.2.8-mc26.2-fabric-{0}.jar' -f (Get-Date -Format 'yyyyMMdd-HHmmss')
    Move-Item -LiteralPath $previousJar -Destination (Join-Path $backupDirectory $backupName)
}

$activeJars = @(Get-ChildItem -LiteralPath $modsDirectory -Filter 'fabric-train-simulator-*.jar' -File)
if ($activeJars.Count -ne 1 -or $activeJars[0].FullName -ne $targetJar) {
    throw 'The mods directory does not contain exactly one active train mod JAR.'
}
if ((Get-FileHash -LiteralPath $targetJar -Algorithm SHA256).Hash -ne $expectedHash) {
    throw 'Final installed JAR hash does not match.'
}

Write-Host 'Fabric Train Simulator 0.2.9 installed and verified. No shutdown was scheduled.'
