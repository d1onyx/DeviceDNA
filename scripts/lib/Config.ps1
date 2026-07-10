#Requires -Version 5.1
# Shared reader for secrets.properties — dot-sourced by the other PowerShell scripts.
# PowerShell port of scripts/lib/config.sh. Values fall back to local.properties and
# keystore.properties so a half-migrated checkout still works.

$script:ConfigRepoRoot = (& git rev-parse --show-toplevel 2>$null)
if ([string]::IsNullOrWhiteSpace($script:ConfigRepoRoot)) {
    $script:ConfigRepoRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
}
$script:ConfigRepoRoot = $script:ConfigRepoRoot.Trim()

$script:ConfigFiles = @(
    (Join-Path $script:ConfigRepoRoot "secrets.properties"),
    (Join-Path $script:ConfigRepoRoot "local.properties"),
    (Join-Path $script:ConfigRepoRoot "keystore.properties")
)

function Read-PropertyFrom {
    param(
        [Parameter(Mandatory = $true)][string]$File,
        [Parameter(Mandatory = $true)][string]$Name
    )

    if (-not (Test-Path -LiteralPath $File)) { return "" }
    foreach ($line in Get-Content -LiteralPath $File) {
        $trimmed = $line.Trim()
        if ($trimmed.StartsWith("#") -or -not $trimmed.Contains("=")) { continue }
        $parts = $trimmed.Split("=", 2)
        if ($parts[0].Trim() -eq $Name) { return $parts[1].Trim() }
    }
    return ""
}

# Upsert into secrets.properties, preserving comments. Rewrites an existing `key=` line
# (commented or not); appends when absent.
function Set-ConfigProperty {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Value
    )

    $file = Join-Path $script:ConfigRepoRoot "secrets.properties"
    if (-not (Test-Path -LiteralPath $file)) {
        throw "Missing secrets.properties. Copy it from secrets.properties.example first."
    }

    $pattern = "^#?\s*" + [regex]::Escape($Name) + "="
    $lines = @(Get-Content -LiteralPath $file)
    $replaced = $false
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match $pattern) {
            $lines[$i] = "$Name=$Value"
            $replaced = $true
            break
        }
    }
    if (-not $replaced) { $lines += "$Name=$Value" }
    Set-Content -LiteralPath $file -Value $lines
}

# Regenerate the derived config files. sync-config.sh is bash-only; on Windows it runs
# under the Git Bash that ships with Git for Windows.
function Invoke-SyncConfig {
    $script = Join-Path $script:ConfigRepoRoot "scripts/sync-config.sh"
    $bash = Get-Command bash -ErrorAction SilentlyContinue
    if (-not $bash) {
        throw "bash not found. Install Git for Windows, or run scripts/sync-config.sh manually."
    }
    & $bash.Source $script | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "sync-config.sh failed" }
}

# First non-empty match across secrets.properties, then the legacy files.
function Get-ConfigProperty {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [string]$FallbackName = ""
    )

    foreach ($file in $script:ConfigFiles) {
        $value = Read-PropertyFrom -File $file -Name $Name
        if (-not [string]::IsNullOrWhiteSpace($value)) { return $value }
    }
    if (-not [string]::IsNullOrWhiteSpace($FallbackName)) {
        return Get-ConfigProperty -Name $FallbackName
    }
    return ""
}
