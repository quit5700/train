param([string[]]$Minecraft = @('1.20.1','1.20.4','1.21.1','1.21.4','1.21.8','1.21.10','1.21.11','26.1.2','26.2','26.3'), [switch]$Offline, [string]$Version='0.2.42')
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$env:GRADLE_USER_HOME = 'H:/CodexWork/q5-gradle-cache'
$taskTemp = Join-Path $env:GRADLE_USER_HOME 'train-formal-temp'
New-Item -ItemType Directory -Path $taskTemp -Force | Out-Null
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Djava.io.tmpdir=$taskTemp"
foreach ($taskMc in $Minecraft) {
    $taskProject = Join-Path $PSScriptRoot "versions/$Version/mc$taskMc"
    if (-not (Test-Path -LiteralPath $taskProject)) { throw "版本目录不存在：$taskProject" }
    Push-Location -LiteralPath $taskProject
    try {
        $taskArgs = @('build','--no-daemon','--no-watch-fs','--console=plain')
        if ($Offline) { $taskArgs += '--offline' }
        & './gradlew.bat' @taskArgs
        if ($LASTEXITCODE -ne 0) { throw "构建失败：$taskMc" }
    } finally { Pop-Location }
}
