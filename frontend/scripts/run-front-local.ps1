$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$previous = @{}
$names = @('BACKEND_URL', 'OPERATOR_USERNAME', 'OPERATOR_PASSWORD', 'SESSION_SECRET')

foreach ($name in $names) {
    $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

try {
    $securePassword = Read-Host 'Contraseña del operador configurada al arrancar el backend' -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $operatorPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
    if ([string]::IsNullOrWhiteSpace($operatorPassword)) {
        throw 'La contraseña del operador no puede estar vacía.'
    }

    $random = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    [Environment]::SetEnvironmentVariable('BACKEND_URL', 'http://localhost:8080', 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_USERNAME', 'operador', 'Process')
    [Environment]::SetEnvironmentVariable('OPERATOR_PASSWORD', $operatorPassword, 'Process')
    [Environment]::SetEnvironmentVariable('SESSION_SECRET', [Convert]::ToBase64String($random), 'Process')
    $operatorPassword = $null

    Push-Location -LiteralPath $projectDirectory
    try {
        & npm.cmd run dev -- --hostname 127.0.0.1
        if ($LASTEXITCODE -ne 0) { throw "Next.js terminó con código $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
} finally {
    foreach ($name in $names) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
}
