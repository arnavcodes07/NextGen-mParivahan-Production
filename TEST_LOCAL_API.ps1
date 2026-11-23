$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8080/api'
Write-Host 'Checking local API health...'
Invoke-RestMethod "$base/health" | Format-List
Write-Host 'Checking demo login...'
$login = @{ mobile = '9560559548'; mpin = '8554' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$base/auth/login" -ContentType 'application/json' -Body $login | Format-List
Write-Host 'Checking vehicle lookup...'
Invoke-RestMethod "$base/vehicles/DL01AB1000" | Format-List
Write-Host 'Checking demo licence lookup...'
Invoke-RestMethod "$base/licences/DL-DEMO-2021-014" | Format-List
Write-Host 'Checking service catalog...'
Invoke-RestMethod "$base/services" | Format-Table
Write-Host 'All basic local API checks completed.'
