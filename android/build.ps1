param([string]$Task = ':app:assembleDebug')
$ErrorActionPreference = 'Stop'
if (-not $env:JAVA_HOME) { $env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr' }
$gradleBat = Join-Path $PSScriptRoot 'gradlew.bat'
$ErrorActionPreference = 'Continue'
& $gradleBat -p $PSScriptRoot $Task --console=plain *> "$PSScriptRoot\build-output.log"
$buildExit = $LASTEXITCODE
Get-Content "$PSScriptRoot\build-output.log" -Tail 65
exit $buildExit
