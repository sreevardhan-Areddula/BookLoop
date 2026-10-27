$ErrorActionPreference = 'Stop'

$mysqlHome = Join-Path $env:USERPROFILE 'mysql-8.4.13-winx64'
$mysqldPath = Join-Path $mysqlHome 'bin\mysqld.exe'
$optionFile = Join-Path $mysqlHome 'my.ini'
$standardOutputLog = Join-Path $mysqlHome 'bookloop-mysql.out.log'
$errorLog = Join-Path $mysqlHome 'bookloop-mysql.err.log'

if (-not (Test-Path -LiteralPath $mysqldPath) -or -not (Test-Path -LiteralPath $optionFile)) {
    throw "The local MySQL server is not set up. Run setup-local-mysql.ps1 first."
}

$optionText = Get-Content -LiteralPath $optionFile -Raw
$portSetting = [regex]::Match($optionText, '(?im)^\s*port\s*=\s*(\d+)\s*$')
if (-not $portSetting.Success) {
    throw "No MySQL port setting was found in '$optionFile'."
}
$mysqlPort = [int]$portSetting.Groups[1].Value

$connectionProbe = New-Object System.Net.Sockets.TcpClient
try {
    $connectionProbe.Connect('127.0.0.1', $mysqlPort)
    Write-Output "MySQL is already running on 127.0.0.1:$mysqlPort."
    return
} catch {
    $connectionProbe.Dispose()
}

$serverProcess = Start-Process `
    -FilePath $mysqldPath `
    -ArgumentList @("--defaults-file=$optionFile", '--console') `
    -PassThru `
    -WindowStyle Hidden `
    -RedirectStandardOutput $standardOutputLog `
    -RedirectStandardError $errorLog

$serverReady = $false
for ($attempt = 0; $attempt -lt 60; $attempt++) {
    if ($serverProcess.HasExited) {
        throw "MySQL exited during startup. Review '$errorLog'."
    }

    $connectionProbe = New-Object System.Net.Sockets.TcpClient
    try {
        $connectionProbe.Connect('127.0.0.1', $mysqlPort)
        $serverReady = $true
        break
    } catch {
        Start-Sleep -Seconds 1
    } finally {
        $connectionProbe.Dispose()
    }
}

if (-not $serverReady) {
    Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    throw "MySQL did not open port $mysqlPort within 60 seconds. Review '$errorLog'."
}

Write-Output "MySQL is running locally on 127.0.0.1:$mysqlPort."
Write-Output "MySQL process ID: $($serverProcess.Id)"
