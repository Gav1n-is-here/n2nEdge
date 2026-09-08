param([ValidateSet('x64','x86')][string]$Architecture='x64',[string]$Toolchain)
$ErrorActionPreference='Stop'
if(-not $Toolchain){$Toolchain=Join-Path $PSScriptRoot $(if($Architecture -eq 'x64'){'toolchains\winlibs\mingw64\bin'}else{'toolchains\winlibs-i686\mingw32\bin'})}
$Toolchain=(Resolve-Path -LiteralPath $Toolchain).Path
$env:PATH=$Toolchain+';'+$env:PATH
$build=Join-Path $PSScriptRoot "build\$Architecture"
$destination=Join-Path $PSScriptRoot "dist\$Architecture"
& "$Toolchain\cmake.exe" -S $PSScriptRoot -B $build -G 'MinGW Makefiles' -DCMAKE_BUILD_TYPE=Release
if($LASTEXITCODE){throw 'Native configuration failed'}
$ErrorActionPreference='Continue'
& "$Toolchain\cmake.exe" --build $build -j 4
$nativeExit=$LASTEXITCODE
$ErrorActionPreference='Stop'
if($nativeExit){throw 'Native build failed'}
$fx='C:\Windows\Microsoft.NET\Framework64\v4.0.30319'
$refs=@('System.dll','System.Core.dll','System.Xml.dll','System.Security.dll','System.Web.Extensions.dll','System.Xaml.dll') | ForEach-Object { '/r:'+(Join-Path $fx $_) }
$refs+=@('PresentationCore.dll','PresentationFramework.dll','WindowsBase.dll') | ForEach-Object { '/r:'+(Join-Path "$fx\WPF" $_) }
New-Item -ItemType Directory -Force $destination | Out-Null
& "$fx\csc.exe" /nologo /target:winexe "/platform:$Architecture" /optimize+ "/out:$destination\n2n-edge.exe" "/win32manifest:$PSScriptRoot\app.manifest" "/win32icon:$PSScriptRoot\assets\icon.ico" $refs "/resource:$PSScriptRoot\Main.xaml,Main.xaml" "/resource:$PSScriptRoot\assets\icon.png,icon.png" "/resource:$build\edge.exe,edge.exe" "/resource:$PSScriptRoot\assets\tap-driver.exe,tap-driver.exe" "$PSScriptRoot\Profile.cs" "$PSScriptRoot\Engine.cs" "$PSScriptRoot\Main.cs"
if($LASTEXITCODE){throw 'Windows UI build failed'}
