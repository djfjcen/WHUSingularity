param(
    [string]$Duration = '1m',
    [string]$Rps = '30000',
    [string]$VuMaxCap = '20000',
    [string]$VuPreCap = '6600',
    [string]$ComposeFile = 'docker-compose.backend.yml',
    [string]$BaselineProductId = '1001',
    [switch]$Cloud,
    [switch]$InfluxDb
)

# 单车道打一台 order-baseline：k6-snag-docker-internal-single-lane.js + BASELINE_PRODUCT_ID，总 RPS = Rps。
# 勿与 internal-baseline 三车道混用：旧版 RpsPerPort×3 已废弃，请直接设 -Rps。
# Prereq: baseline 已起；Redis：.\refill-baseline-stock.ps1
# -Cloud / -InfluxDb: same as run-k6-order-load-one.ps1.

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ($Cloud) {
    if (-not $env:K6_CLOUD_TOKEN) { throw 'Use -Cloud only after setting $env:K6_CLOUD_TOKEN.' }
    if (-not $env:K6_CLOUD_STACK_ID) { throw 'Use -Cloud only after setting $env:K6_CLOUD_STACK_ID.' }
    if (-not $env:K6_CLOUD_PROJECT_ID) { throw 'Use -Cloud only after setting $env:K6_CLOUD_PROJECT_ID.' }
}

$base = .\k6-print-order-baseline-targets.ps1 -Count 1
if (-not $base) { throw 'ORDER_BASE empty; start singularity-order-baseline container.' }

$dockerArgs = @(
    '-f', $ComposeFile,
    '--profile', 'k6', 'run', '--rm', '--no-deps',
    '-e', "ORDER_BASE=$base",
    '-e', "BASELINE_PRODUCT_ID=$BaselineProductId",
    '-e', "DURATION=$Duration",
    '-e', "RPS=$Rps",
    '-e', "VU_MAX_CAP=$VuMaxCap",
    '-e', "VU_PRE_CAP=$VuPreCap",
    '-e', 'SUMMARY_OUT=/out/summary-docker-baseline.json'
)
if ($Cloud) {
    $dockerArgs += '-e', "K6_CLOUD_TOKEN=$($env:K6_CLOUD_TOKEN)"
    $dockerArgs += '-e', "K6_CLOUD_STACK_ID=$($env:K6_CLOUD_STACK_ID)"
    $dockerArgs += '-e', "K6_CLOUD_PROJECT_ID=$($env:K6_CLOUD_PROJECT_ID)"
}
if ($InfluxDb) {
    $url = $env:K6_INFLUXDB_URL
    if ([string]::IsNullOrWhiteSpace($url)) { $url = 'http://influxdb:8086/k6' }
    $dockerArgs += '-e', "K6_INFLUXDB_URL=$url"
}
$dockerArgs += 'k6-order-baseline-load-one'
& docker compose @dockerArgs
