param(
    [int]$Port = 3306
)

$ErrorActionPreference = 'Stop'

$profileRoot = $env:USERPROFILE
$extractedHome = Join-Path $profileRoot 'Downloads\mysql-8.4.13-winx64'
$mysqlHome = Join-Path $profileRoot 'mysql-8.4.13-winx64'
$mysqldPath = Join-Path $mysqlHome 'bin\mysqld.exe'
$mysqlPath = Join-Path $mysqlHome 'bin\mysql.exe'
$dataDirectory = Join-Path $mysqlHome 'data-8.4'
$optionFile = Join-Path $mysqlHome 'my.ini'
$envFile = Join-Path $PSScriptRoot '.env.local'
$standardOutputLog = Join-Path $mysqlHome 'bookloop-mysql.out.log'
$errorLog = Join-Path $mysqlHome 'bookloop-mysql.err.log'

if ($Port -lt 1 -or $Port -gt 65535) {
    throw "Port must be between 1 and 65535."
}

if (-not (Test-Path -LiteralPath $mysqlHome)) {
    if (-not (Test-Path -LiteralPath (Join-Path $extractedHome 'bin\mysqld.exe'))) {
        throw "Could not find the extracted MySQL ZIP at '$extractedHome'."
    }

    if (Test-Path -LiteralPath $mysqlHome) {
        throw "The destination '$mysqlHome' already exists. No files were moved."
    }

    Move-Item -LiteralPath $extractedHome -Destination $mysqlHome
}

if (-not (Test-Path -LiteralPath $mysqldPath) -or -not (Test-Path -LiteralPath $mysqlPath)) {
    throw "MySQL server or client executable is missing from '$mysqlHome'."
}

if (Test-Path -LiteralPath $envFile) {
    throw "'$envFile' already exists. Setup stopped without replacing it."
}

if (Test-Path -LiteralPath $dataDirectory) {
    $existingDataFiles = @(Get-ChildItem -LiteralPath $dataDirectory -Force)
    if ($existingDataFiles.Count -gt 0) {
        throw "The MySQL data directory already contains files at '$dataDirectory'. Setup stopped without changing it."
    }
}

$mysqlHomeForIni = $mysqlHome.Replace('\', '/')
$dataDirectoryForIni = $dataDirectory.Replace('\', '/')
@(
    '[mysqld]'
    "basedir=$mysqlHomeForIni"
    "datadir=$dataDirectoryForIni"
    "port=$Port"
    'bind-address=127.0.0.1'
) | Set-Content -LiteralPath $optionFile -Encoding ascii

$initializationStdout = Join-Path $mysqlHome 'mysql-initialize.out.tmp'
$initializationStderr = Join-Path $mysqlHome 'mysql-initialize.err.tmp'
$initializationProcess = Start-Process `
    -FilePath $mysqldPath `
    -ArgumentList @("--defaults-file=$optionFile", '--initialize', '--console') `
    -PassThru `
    -Wait `
    -WindowStyle Hidden `
    -RedirectStandardOutput $initializationStdout `
    -RedirectStandardError $initializationStderr
$initializationOutput = (Get-Content -LiteralPath $initializationStdout -Raw -ErrorAction SilentlyContinue) + (Get-Content -LiteralPath $initializationStderr -Raw -ErrorAction SilentlyContinue)
$initializationExitCode = $initializationProcess.ExitCode
Remove-Item -LiteralPath $initializationStdout,$initializationStderr -Force -ErrorAction SilentlyContinue
$temporaryPasswordMatch = [regex]::Match(
    $initializationOutput,
    '(?i)temporary password is generated for root@localhost:\s*(\S+)'
)

if ($initializationExitCode -ne 0 -or -not $temporaryPasswordMatch.Success) {
    $safeInitializationOutput = $initializationOutput -replace '(?im)(temporary password is generated for root@localhost:\s*)\S+', '$1<redacted>'
    throw "MySQL initialization failed. Output: $safeInitializationOutput"
}

$temporaryRootPassword = $temporaryPasswordMatch.Groups[1].Value
$randomBytes = New-Object byte[] 32
$randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$randomGenerator.GetBytes($randomBytes)
$applicationPassword = [System.BitConverter]::ToString($randomBytes).Replace('-', '')
$randomGenerator.GetBytes($randomBytes)
$newRootPassword = [System.BitConverter]::ToString($randomBytes).Replace('-', '')
$randomGenerator.Dispose()

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
        $connectionProbe.Connect('127.0.0.1', $Port)
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
        throw "MySQL did not open port $Port within 60 seconds. Review '$errorLog'."
}

$env:MYSQL_PWD = $temporaryRootPassword
$sqlInput = Join-Path $mysqlHome 'mysql-initialize.sql.tmp'
$sqlOutput = Join-Path $mysqlHome 'mysql-client.out.tmp'
$sqlError = Join-Path $mysqlHome 'mysql-client.err.tmp'
$sql = @"
ALTER USER 'root'@'localhost' IDENTIFIED BY '$newRootPassword';
CREATE DATABASE bookloop_dev CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'bookloop_app'@'localhost' IDENTIFIED BY '$applicationPassword';
GRANT ALL PRIVILEGES ON bookloop_dev.* TO 'bookloop_app'@'localhost';
"@
[System.IO.File]::WriteAllText($sqlInput, $sql, (New-Object System.Text.UTF8Encoding($false)))

try {
    $databaseSetupProcess = Start-Process `
        -FilePath $mysqlPath `
        -ArgumentList @('--connect-expired-password', '--protocol=TCP', '--host=127.0.0.1', "--port=$Port", '--user=root') `
        -PassThru `
        -Wait `
        -WindowStyle Hidden `
        -RedirectStandardInput $sqlInput `
        -RedirectStandardOutput $sqlOutput `
        -RedirectStandardError $sqlError
    $databaseSetupOutput = (Get-Content -LiteralPath $sqlOutput -Raw -ErrorAction SilentlyContinue) + (Get-Content -LiteralPath $sqlError -Raw -ErrorAction SilentlyContinue)
    $databaseSetupExitCode = $databaseSetupProcess.ExitCode
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $sqlInput,$sqlOutput,$sqlError -Force -ErrorAction SilentlyContinue
}

if ($databaseSetupExitCode -ne 0) {
    Stop-Process -Id $serverProcess.Id -Force -ErrorAction SilentlyContinue
    throw "MySQL database setup failed. Output: $databaseSetupOutput"
}

$envLines = @(
    "SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:$Port/bookloop_dev"
    'SPRING_DATASOURCE_USERNAME=bookloop_app'
    "SPRING_DATASOURCE_PASSWORD=$applicationPassword"
)
[System.IO.File]::WriteAllLines($envFile, $envLines, (New-Object System.Text.UTF8Encoding($false)))

Write-Output "MySQL is running locally on 127.0.0.1:$Port."
Write-Output "Database and app account are ready. App credentials are stored in '$envFile'."
Write-Output "MySQL process ID: $($serverProcess.Id)"
