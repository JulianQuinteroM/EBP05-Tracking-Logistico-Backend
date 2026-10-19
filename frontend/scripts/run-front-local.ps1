$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$previous = @{}
$names = @('BACKEND_URL', 'OPERATOR_USERNAME', 'OPERATOR_PASSWORD', 'ADMIN_USERNAME', 'ADMIN_PASSWORD', 'VIEWER_USERNAME', 'VIEWER_PASSWORD', 'SESSION_SECRET')

foreach ($name in $names) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    function Read-TemporarySecret {
        param([string]$Prompt)
        $secret = Read-Host $Prompt -AsSecureString
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
    }
    $operatorPassword = Read-TemporarySecret 'Contraseña del operador configurada en el backend'
    $adminPassword = Read-TemporarySecret 'Contraseña del administrador configurada en el backend'
    $viewerPassword = Read-TemporarySecret 'Contraseña de consulta configurada en el backend'
    if ($operatorPassword.Length -lt 12 -or $adminPassword.Length -lt 12 -or $viewerPassword.Length -lt 12) {
        throw 'Cada contraseña necesita al menos 12 caracteres.'
    }

    $random = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    [Environment]::SetEnvironmentVariable('BACKEND_URL', 'http://localhost:8080', 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_USERNAME', 'operador', 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_PASSWORD', $operatorPassword, 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_USERNAME', 'administrador', 'Process')
    [Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $adminPassword, 'Process')
    [Environment]::SetEnvironmentVariable('VIEWER_USERNAME', 'consulta', 'Process')
    [Environment]::SetEnvironmentVariable('VIEWER_PASSWORD', $viewerPassword, 'Process')
    [Environment]::SetEnvironmentVariable('SESSION_SECRET', [Convert]::ToBase64String($random), 'Process')
    $operatorPassword = $null
    $adminPassword = $null
    $viewerPassword = $null

    Push-Location -LiteralPath $projectDirectory
    try {
        & npm.cmd run dev -- --hostname localhost
        if ($LASTEXITCODE -ne 0) { throw "Next.js terminó con código $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($name in $names) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
}
