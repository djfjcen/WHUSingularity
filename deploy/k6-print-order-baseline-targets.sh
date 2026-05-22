#!/usr/bin/env bash
# 输出 baseline order 容器基址（前 N 个，端口 8085）
set -euo pipefail
COUNT="${1:-1}"
mapfile -t names < <(docker ps --filter "label=com.docker.compose.service=singularity-order-baseline" --format '{{.Names}}' | sort -u)
if (("${#names[@]}" < COUNT)); then
  echo "need at least ${COUNT} singularity-order-baseline containers, got ${#names[@]}" >&2
  exit 1
fi
bases=()
for ((i = 0; i < COUNT; i++)); do
  bases+=("http://${names[i]}:8085")
done
IFS=','; echo "${bases[*]}"
