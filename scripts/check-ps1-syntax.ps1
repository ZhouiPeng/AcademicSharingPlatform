$Path = if ($args.Count -ge 1) { $args[0] } else { 'scripts\\recompose-remote-service-fixed.ps1' }
$content = Get-Content -Raw -Path $Path
$tokensRef = [ref]$null
$errorsRef = [ref]@()
[System.Management.Automation.Language.Parser]::ParseInput($content, $tokensRef, $errorsRef)
if ($errorsRef.Value -and $errorsRef.Value.Count -gt 0) {
    $errorsRef.Value | ForEach-Object { Write-Error $_.Message }
    exit 1
} else {
    Write-Output 'No syntax errors'
    exit 0
}
