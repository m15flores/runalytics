#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# create-topics.sh
#
# Creates all required Kafka topics in Redpanda via rpk.
# Run this once after Redpanda starts on the VPS.
#
# Usage:
#   ./scripts/create-topics.sh
#
# Requirements:
#   - Redpanda container is running and healthy
#   - rpk is available inside the container (it ships with Redpanda)
#
# Topics created:
#   Core pipeline topics  (1 partition, replication-factor 1 — single broker)
#   DLQ topics            (same config, 7-day retention)
# ─────────────────────────────────────────────────────────────────────────────

set -euo pipefail

CONTAINER="${REDPANDA_CONTAINER:-redpanda-runalytics}"

# Retention for DLQ topics: 7 days in milliseconds
DLQ_RETENTION_MS=$((7 * 24 * 60 * 60 * 1000))

# ─── helpers ─────────────────────────────────────────────────────────────────

log()  { echo "[$(date '+%H:%M:%S')] $*"; }
info() { log "INFO  $*"; }
ok()   { log "OK    $*"; }

create_topic() {
  local topic="$1"
  local extra_flags="${2:-}"

  if docker exec "$CONTAINER" rpk topic describe "$topic" \
       > /dev/null 2>&1; then
    info "topic already exists — skipping: $topic"
    return 0
  fi

  # shellcheck disable=SC2086
  docker exec "$CONTAINER" rpk topic create "$topic" \
    --partitions 1 \
    --replicas 1 \
    $extra_flags

  ok "created: $topic"
}

# ─── wait for Redpanda to be ready ───────────────────────────────────────────

info "waiting for Redpanda to be ready..."
until docker exec "$CONTAINER" rpk cluster health 2>/dev/null \
    | grep -q "Healthy:.*true"; do
  sleep 2
done
info "Redpanda is healthy"

# ─── core pipeline topics ─────────────────────────────────────────────────────

info "creating core pipeline topics..."
create_topic "activities.raw.ingested"
create_topic "activities.normalized"
create_topic "metrics.calculated"
create_topic "reports.generated"
create_topic "recommendations.generated"

# ─── DLQ topics ───────────────────────────────────────────────────────────────
# TODO: DLQ consumers are not yet wired in any service — these topics are
#       created now so they are ready when DLQ handling is implemented.

info "creating DLQ topics (retention: 7 days)..."
DLQ_FLAGS="-c retention.ms=${DLQ_RETENTION_MS}"
create_topic "activities.raw.ingested.dlq"   "$DLQ_FLAGS"
create_topic "activities.normalized.dlq"     "$DLQ_FLAGS"
create_topic "metrics.calculated.dlq"        "$DLQ_FLAGS"
create_topic "reports.generated.dlq"         "$DLQ_FLAGS"

# ─── summary ──────────────────────────────────────────────────────────────────

info "listing all topics:"
docker exec "$CONTAINER" rpk topic list

info "done — all topics created successfully"