#!/usr/bin/env bash
# Baseline Redis 库存键默认 baseline:stock:1001
set -euo pipefail
QTY="${1:-1000000}"
KEY="${BASELINE_STOCK_KEY:-baseline:stock:1001}"
C="${REDIS_CONTAINER:-singularity-redis}"
echo "SET $KEY = $QTY in $C"
docker exec "$C" redis-cli SET "$KEY" "$QTY"
echo Done.
