<#!
.SYNOPSIS
Generates a cryptographically random JWT secret for a production secret store.

.DESCRIPTION
Prints an URL-safe 64-character random value. It never writes a secret to the
repository. Copy the output directly to the production deployment's JWT_SECRET.
#>
[CmdletBinding()]
param()

$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
} finally {
    $rng.Dispose()
}

[Convert]::ToBase64String($bytes).Replace('+', '-').Replace('/', '_').TrimEnd('=')
