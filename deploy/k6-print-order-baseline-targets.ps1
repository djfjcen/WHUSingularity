# Print comma-separated http://<container>:8085 for first N singularity-order-baseline replicas (docker ps, sorted by name).
param(
    [int]$Count = 1
)
$ErrorActionPreference = "Stop"
$arr = @(
    docker ps --filter "label=com.docker.compose.service=singularity-order-baseline" --format "{{.Names}}" |
        Where-Object { $_ } |
        Sort-Object
)
if ($arr.Count -lt $Count) {
    Write-Error "Need $Count singularity-order-baseline containers, got $($arr.Count). Run: docker compose -f deploy/docker-compose.backend.yml up -d singularity-order-baseline"
    exit 1
}
(0..($Count - 1) | ForEach-Object { "http://$($arr[$_]):8085" }) -join ","
