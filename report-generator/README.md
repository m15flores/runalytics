# report-generator

Microservice responsible for generating weekly training reports from activity metrics.
Part of the [Runaytics](../README.md) platform.

---

## What it does

Consumes `metrics.calculated` Kafka events, aggregates the last 4 weeks of training data
for each athlete, generates a detailed markdown report, and publishes a `reports.generated`
event for the AI coach downstream.

Also exposes a REST API to manage athlete profiles (age, weight, HR zones, training goal).

### Pipeline position

```
metrics-engine
    → [metrics.calculated]
        → report-generator
            → [reports.generated]
                → ai-coach
```

---

## Tech stack

- **Java 17** + Spring Boot 3.5.8
- **Spring Kafka** — event consumption and publication
- **Spring Data JPA + Hibernate 6** — PostgreSQL persistence
- **MapStruct** — DTO ↔ Entity mapping
- **Lombok** — boilerplate reduction
- **Testcontainers** — PostgreSQL + Kafka for integration tests

---

## Getting started

### Prerequisites

- Docker (for PostgreSQL and Kafka)
- Java 17+
- Maven 3.9+

### Run locally

```bash
# Start infrastructure
docker compose up kafka postgres

# Run the service
cd report-generator
mvn spring-boot:run
```

The service starts on **port 8083**.

### Run tests

```bash
cd report-generator
mvn test
```

Tests use Testcontainers (real PostgreSQL and Kafka). No H2, no mocks for infrastructure.

---

## Configuration

Key properties in `application.yml`:

```yaml
server:
  port: 8083

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/report_generator
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: report-generator-group

app:
  timezone: Europe/Paris
  kafka:
    topics:
      metrics-calculated: metrics.calculated
      reports-generated: reports.generated
      reports-dlq: reports.dlq
```

---

## Kafka topics

| Direction | Topic | Key | Payload |
|-----------|-------|-----|---------|
| Consumes | `metrics.calculated` | userId | `ActivityMetricsDto` |
| Produces | `reports.generated` | userId | `ReportGeneratedEventDto` |
| DLQ | `reports.dlq` | userId | `ActivityMetricsDto` (on consumer failure) |

### `ActivityMetricsDto` (inbound)

```json
{
  "activityId": "uuid",
  "userId": "string",
  "startedAt": "2024-12-08T10:00:00Z",
  "totalDistance": 10.5,
  "totalDuration": 3600,
  "averagePace": 343,
  "averageHeartRate": 145,
  "averageCadence": 170,
  "hrZones": { "Z1": 120, "Z2": 2400, "Z3": 900, "Z4": 180, "Z5": 0 },
  "totalCalories": 650,
  "totalAscent": 85,
  "totalDescent": 85
}
```

### `ReportGeneratedEventDto` (outbound)

```json
{
  "reportId": "uuid",
  "userId": "string",
  "weekNumber": 50,
  "year": 2024,
  "summaryJson": "{ ... }",
  "triggerActivityId": "uuid",
  "generatedAt": "2024-12-10T12:00:00Z"
}
```

---

## REST API

Base URL: `http://localhost:8083`

### Athlete Profile

| Method | Path | Body | Response | Description |
|--------|------|------|----------|-------------|
| `POST` | `/api/profiles` | `AthleteProfileDto` | 201 / 409 | Create profile |
| `GET` | `/api/profiles/user/{userId}` | — | 200 / 404 | Get profile |
| `PUT` | `/api/profiles/user/{userId}` | `AthleteProfileDto` | 200 / 404 | Update profile |
| `DELETE` | `/api/profiles/user/{userId}` | — | 204 / 404 | Delete profile |
| `GET` | `/api/profiles` | — | 200 | List all profiles |

### `AthleteProfileDto`

```json
{
  "userId": "mario-runner",
  "name": "Mario",
  "age": 30,
  "weight": 70.0,
  "maxHeartRate": 190,
  "currentGoal": "Marathon sub-3:30"
}
```

Validation rules:
- `userId`: required, not blank
- `name`: required, not blank
- `age`: 10–120 (optional)
- `weight`: positive (optional)
- `maxHeartRate`: 100–250 (optional)
- `currentGoal`: free text (optional)

### Error responses

```json
{
  "code": "NOT_FOUND",
  "message": "Profile not found for userId: mario-runner",
  "timestamp": 1733650000000
}
```

HTTP status codes:
- `404 NOT_FOUND` — profile does not exist
- `409 CONFLICT` — profile already exists for that userId
- `400 BAD_REQUEST` — Bean Validation failure

---

## Database schema

### `athlete_profile`

```sql
CREATE TABLE athlete_profile (
    id             UUID PRIMARY KEY,
    user_id        VARCHAR UNIQUE NOT NULL,
    name           VARCHAR NOT NULL,
    age            INTEGER,
    weight         DOUBLE PRECISION,
    max_heart_rate INTEGER,
    current_goal   VARCHAR,
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL
);
```

### `activity_metrics`

Read-only table. Written by the `metrics-engine` service, read here for aggregation.
Key columns: `activity_id`, `user_id`, `started_at`, `total_distance`, `total_duration`,
`average_pace`, `average_heart_rate`, `hr_zones` (JSONB).

### `training_reports`

```sql
CREATE TABLE training_reports (
    id                   UUID PRIMARY KEY,
    trigger_activity_id  UUID NOT NULL,
    user_id              VARCHAR NOT NULL,
    week_number          INTEGER NOT NULL,
    year                 INTEGER NOT NULL,
    markdown_content     TEXT NOT NULL,
    summary_json         TEXT,
    created_at           TIMESTAMP NOT NULL,
    CONSTRAINT uq_report_week UNIQUE (user_id, week_number, year)
);
```

The unique constraint on `(user_id, week_number, year)` ensures one report per athlete per
week. Re-processing an existing week **updates** the report in place (never DELETE+INSERT).

---

## Report generation logic

### Week calculation

Weeks follow **ISO-8601** (Monday = first day, minimum 4 days in first week).
Week and year are derived from the activity's `startedAt` timestamp in the `Europe/Paris`
timezone — not from the current system time.

```
activityTime (UTC) → Europe/Paris → ISO week number + year
```

### Aggregation window

The last 4 weeks of activity data are aggregated for context. The current week's data
is at index 0 of the returned list.

### Trend calculation

Distance trend is computed by comparing the current week against the previous week:
- `> +2%` → "improving"
- `< -2%` → "declining"
- otherwise → "stable"

### Markdown report structure

```markdown
# Training Report - Week 50/2024

## This Week Summary
...

## Last 4 Weeks Comparison
| Week | Distance | Duration | Pace | HR |
...

## Detailed Analysis
...

## Heart Rate Zones
...

## Goals
...
```

---

## Key design decisions

**No DELETE+INSERT on report regeneration.** The unique constraint `(user_id, week_number, year)`
would throw on a concurrent re-insert. Reports are always found-or-created and then updated
via `save()`, which Hibernate resolves to UPDATE or INSERT accordingly.

**Clock injection everywhere.** All `Instant.now()` calls use an injected `java.time.Clock`
bean. This makes timestamp behaviour fully deterministic in tests via `Clock.fixed(...)`.

**No service-to-service HTTP.** The profile REST API is for external callers (frontend).
Internal communication with other Runaytics services is Kafka-only.

**Activity metrics are read-only here.** The `activity_metrics` table is shared with
`metrics-engine`. This service only reads from it using a `@Immutable` JPA entity.

---

## Known limitations

- Package name uses `com.runalitycs` (typo) — should be `com.runalytics`. Pending rename.
- DLQ topic `reports.dlq` is produced to on consumer failure but never consumed (no monitoring).
- `ClockConfig` uses `Clock.systemDefaultZone()` — depends on JVM default timezone.
- Timezone `Europe/Paris` is hardcoded as a constant in two services instead of injected from config.