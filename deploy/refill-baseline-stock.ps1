# Baseline stock is only in Redis (default key baseline:stock:1001).
# Default: redis-cli SET. Optional: POST /api/order/admin/stock/reset on order-baseline (-Mode http -BaselineBaseUrl ...).
param(
    [long]$Quantity = 5000000,
    [string]$RedisKey = 'baseline:stock:1001',
    [string]$RedisContainer = 'singularity-redis',
    [string]$BaselineBaseUrl = '',
    [ValidateSet('redis', 'http')]
    [string]$Mode = 'redis'
)
$ErrorActionPreference = 'Stop'

if ($Mode -eq 'http') {
    if ([string]::IsNullOrWhiteSpace($BaselineBaseUrl)) {
        throw "Mode=http requires -BaselineBaseUrl (e.g. http://localhost:8085)."
    }
    $uri = "$($BaselineBaseUrl.TrimEnd('/'))/api/order/admin/stock/reset"
    Write-Host "POST $uri quantity=$Quantity"
    $body = @{ quantity = $Quantity } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri $uri -ContentType 'application/json' -Body $body | ConvertTo-Json
}
else {
    Write-Host "SET $RedisKey = $Quantity (redis-cli in $RedisContainer)"
    docker exec $RedisContainer redis-cli SET $RedisKey $Quantity | Out-Host
}

Write-Host 'Done.'
