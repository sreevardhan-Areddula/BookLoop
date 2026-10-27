$ErrorActionPreference = 'Stop'
$envFile = Join-Path $PSScriptRoot '.env.local'

if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Local database settings were not found. Run setup-local-mysql.ps1 first."
}

& (Join-Path $PSScriptRoot 'start-local-mysql.ps1')

Get-Content -LiteralPath $envFile | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        Set-Item -Path "Env:$($matches[1])" -Value $matches[2]
    }
}

& mvn -f (Join-Path $PSScriptRoot 'pom.xml') spring-boot:run
exit $LASTEXITCODE
