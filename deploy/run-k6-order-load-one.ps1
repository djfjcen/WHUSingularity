param(
    [string]$Duration = '1m',
    [string]$Rps = '20000',
    [string]$ProductId = '',
    [string]$VuMaxCap = '20000',
    [string]$VuPreCap = '6600',
    [string]$ComposeFile = 'docker-compose.backend.yml',
    [switch]$Cloud,
    [switch]$InfluxDb
)

# 单车道打一台 order：k6-snag-docker-internal-single-lane.js，总 RPS = Rps。
# 默认不传 productId → snagOrder / allocate + LoggingInterceptor + handler。
# -ProductId PROD_001 → snagOrderByProduct（无 interceptor 链指标）。
# Prereq: >=1 order（建议 loadtest）；Redis：.\refill-stock-buckets.ps1

$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

if ($Cloud) {
    if (-not $env:K6_CLOUD_TOKEN) { throw 'Use -Cloud only after setting $env:K6_CLOUD_TOKEN.' }
    if (-not $env:K6_CLOUD_STACK_ID) { throw 'Use -Cloud only after setting $env:K6_CLOUD_STACK_ID.' }
    if (-not $env:K6_CLOUD_PROJECT_ID) { throw 'Use -Cloud only after setting $env:K6_CLOUD_PROJECT_ID (Grafana Cloud k6 project id).' }
}

$base = .\k6-print-order-targets.ps1 -Count 1
if (-not $base) { throw 'ORDER_BASE empty; need at least one singularity-order container.' }

$dockerArgs = @(
    '-f', $ComposeFile,
    '--profile', 'k6', 'run', '--rm', '--no-deps',
    '-e', "ORDER_BASE=$base",
    '-e', "DURATION=$Duration",
    '-e', "RPS=$Rps",
    '-e', "VU_MAX_CAP=$VuMaxCap",
    '-e', "VU_PRE_CAP=$VuPreCap",
    '-e', 'SUMMARY_OUT=/out/summary-docker-order-one.json'
)
if (-not [string]::IsNullOrWhiteSpace($ProductId)) {
    $dockerArgs += '-e', "ORDER_PRODUCT_ID=$ProductId"
}
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
$dockerArgs += 'k6-order-load-one'
& docker compose @dockerArgs
