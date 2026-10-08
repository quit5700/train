param([ValidateSet('1.20.1','1.20.4')][string]$MinecraftVersion='1.20.1')
$ErrorActionPreference='Stop'
$projectRoot=Split-Path -Parent $PSScriptRoot
$versionRoot=Join-Path $projectRoot ('mc'+$MinecraftVersion)
$previousOptions=$env:JAVA_TOOL_OPTIONS
$previousConsoleEncoding=[Console]::OutputEncoding
try {
    chcp 65001 | Out-Null
    [Console]::OutputEncoding=New-Object Text.UTF8Encoding($false)
    $env:JAVA_TOOL_OPTIONS=($previousOptions+' -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8').Trim()
    Push-Location -LiteralPath $versionRoot
    try {
        if($MinecraftVersion -eq '1.20.1') {
            $init=Join-Path $PSScriptRoot 'verified-lwjgl.init.gradle'
            if(-not(Test-Path -LiteralPath (Join-Path $projectRoot 'outputs/verified-lwjgl-repository'))) { throw '缺少本地LWJGL依赖，请先解压交接中的构建依赖包到工程根。' }
            & '.\gradlew.bat' -I $init test build --offline --no-daemon --console=plain
        } else {
            & '.\gradlew.bat' test build --offline --no-daemon --console=plain
        }
        if($LASTEXITCODE -ne 0) { throw '测试或构建失败，请查看输出。' }
    } finally { Pop-Location }
} finally {
    $env:JAVA_TOOL_OPTIONS=$previousOptions
    [Console]::OutputEncoding=$previousConsoleEncoding
}