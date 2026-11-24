$ErrorActionPreference = 'Stop'
$projectDirectory = Split-Path -Parent $PSScriptRoot
$previousBackend = [Environment]::GetEnvironmentVariable('BACKEND_URL', 'Process')
$previousSecret = [Environment]::GetEnvironmentVariable('SESSION_SECRET', 'Process')
try {
    $random = [byte[]]::new(32)
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($random) } finally { $generator.Dispose() }
    [Environment]::SetEnvironmentVariable('BACKEND_URL', 'http://localhost:8080', 'Process')
    [Environment]::SetEnvironmentVariable('SESSION_SECRET', [Convert]::ToBase64String($random), 'Process')
    Write-Host 'Frontend local: http://localhost:3000. Inicia sesión con una cuenta del backend.'
    Push-Location -LiteralPath $projectDirectory
    try {
        & npm.cmd run dev -- --hostname localhost
        if ($LASTEXITCODE -ne 0) { throw "Next.js terminó con código $LASTEXITCODE" }
    } finally { Pop-Location }
} finally {
    [Environment]::SetEnvironmentVariable('BACKEND_URL', $previousBackend, 'Process')
    [Environment]::SetEnvironmentVariable('SESSION_SECRET', $previousSecret, 'Process')
}
