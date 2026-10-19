param(
    [ValidateRange(1, 65535)]
    [int]$PostgresPort = 5432
)

$ErrorActionPreference = 'Stop'
$backendDirectory = Split-Path -Parent $PSScriptRoot
$environmentNames = @('DB_URL', 'DB_USERNAME', 'DB_PASSWORD', 'OPERATOR_USERNAME', 'OPERATOR_PASSWORD', 'ADMIN_USERNAME', 'ADMIN_PASSWORD', 'VIEWER_USERNAME', 'VIEWER_PASSWORD', 'FRONTEND_ORIGIN')
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

function Convert-TemporarySecret {
    param([System.Security.SecureString]$Secret)
    $temporaryPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secret)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($temporaryPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($temporaryPointer)
    }
}

try {
    Write-Host 'Arranque local del backend. Las contraseñas no se guardarán en archivos ni en el historial de comandos.'
    $databaseSecurePassword = Read-Host 'Contraseña del rol PostgreSQL tracking' -AsSecureString
    $operatorSecurePassword = Read-Host 'Elige una contraseña de operador (mínimo 12 caracteres)' -AsSecureString
    $adminSecurePassword = Read-Host 'Elige una contraseña de administrador (mínimo 12 caracteres)' -AsSecureString
    $viewerSecurePassword = Read-Host 'Elige una contraseña de consulta (mínimo 12 caracteres)' -AsSecureString
    $databasePlainPassword = Convert-TemporarySecret $databaseSecurePassword
    $operatorPlainPassword = Convert-TemporarySecret $operatorSecurePassword
    $adminPlainPassword = Convert-TemporarySecret $adminSecurePassword
    $viewerPlainPassword = Convert-TemporarySecret $viewerSecurePassword
    if ($operatorPlainPassword.Length -lt 12 -or $adminPlainPassword.Length -lt 12 -or $viewerPlainPassword.Length -lt 12) {
        throw 'Cada contraseña necesita al menos 12 caracteres.'
    }

    [Environment]::SetEnvironmentVariable('DB_URL', "jdbc:postgresql://localhost:$PostgresPort/tracking_logistico", 'Process')
    [Environment]::SetEnvironmentVariable('DB_USERNAME', 'tracking', 'Process')
    [Environment]::SetEnvironmentVariable('DB_PASSWORD', $databasePlainPassword, 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_USERNAME', 'operador', 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_PASSWORD', $operatorPlainPassword, 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_USERNAME', 'administrador', 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $adminPlainPassword, 'Process')
    [Environment]::SetEnvironmentVariable('VIEWER_USERNAME', 'consulta', 'Process')
    [Environment]::SetEnvironmentVariable('VIEWER_PASSWORD', $viewerPlainPassword, 'Process')
    [Environment]::SetEnvironmentVariable('FRONTEND_ORIGIN', 'http://localhost:3000', 'Process')

    Push-Location -LiteralPath $backendDirectory
    try {
        $localMavenSettings = Join-Path $backendDirectory '.mvn/settings.xml'
        if (Test-Path -LiteralPath $localMavenSettings) {
            & mvn -s $localMavenSettings spring-boot:run
        }
        else {
            & mvn spring-boot:run
        }
        if ($LASTEXITCODE -ne 0) {
            throw "El backend terminó con código $LASTEXITCODE. Revisa el error mostrado arriba."
        }
    }
    finally {
        Pop-Location
    }
}
finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], 'Process')
    }
    Remove-Variable databasePlainPassword, operatorPlainPassword, adminPlainPassword, viewerPlainPassword -ErrorAction SilentlyContinue
}
