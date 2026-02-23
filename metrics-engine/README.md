# Metrics Engine

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-6DB33F?logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.3-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)

Consumes normalized activity data from Kafka, calculates training metrics (pace, GAP, HR zones, cadence, power), persists results to PostgreSQL, and publishes downstream events to `metrics.calculated`.

## Data Flow

1. Consumes structured activity event from `activities.normalized`
2. Validates `activityId`, `userId` and `session` — invalid messages are acknowledged and skipped (no retry)
3. Checks for duplicate `activityId` — already-processed activities are silently skipped
4. Calculates activity-level metrics (pace, GAP, HR zones, min HR, max pace from laps)
5. Calculates per-lap metrics (pace, GAP, lap name from intensity)
6. Persists `ActivityMetrics` + `LapMetrics` to PostgreSQL
7. Publishes `ActivityMetricsDto` to `metrics.calculated`

## Kafka

| Direction | Topic | Partition key |
|-----------|-------|---------------|
| Consumes | `activities.normalized` | — |
| Produces | `metrics.calculated` | `activityId` |
| DLQ | `activities.normalized.dlq` | `activityId` |

- Consumer group: `metrics-engine-group`
- ACK mode: `MANUAL_IMMEDIATE`
- Retry: 3 attempts, 2 s backoff
- Non-retryable: `IllegalArgumentException`, `NullPointerException`, `DeserializationException`

### Input — `activities.normalized`

```json
{
  "activityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "user-12345",
  "device": "Garmin Fenix 7",
  "startedAt": "2025-01-01T10:30:00Z",
  "session": {
    "totalDistance": 13138.37,
    "totalTimerTime": 4777,
    "totalElapsedTime": 4987,
    "totalCalories": 1048,
    "avgHeartRate": 140,
    "maxHeartRate": 150,
    "avgCadence": 78,
    "maxCadence": 84,
    "totalAscent": 36,
    "totalDescent": 34,
    "timeInHrZones": { "Z1": 0, "Z2": 2580, "Z3": 2010, "Z4": 187, "Z5": 0 }
  },
  "laps": [
    {
      "lapNumber": 1,
      "totalDistance": 1000.0,
      "totalTimerTime": 420,
      "avgHeartRate": 130,
      "intensity": "warmup"
    }
  ],
  "samples": [
    {
      "timestamp": "2025-01-01T10:30:00Z",
      "heartRate": 125,
      "cadence": 150,
      "altitude": 650.0,
      "distance": 100.0
    }
  ],
  "normalizedAt": "2025-01-01T10:32:00Z"
}
```

### Output — `metrics.calculated`

```json
{
  "activityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "user-12345",
  "totalDistance": 13138.37,
  "totalDuration": 4777,
  "totalCalories": 1048,
  "averagePace": 364,
  "maxPace": 300,
  "averageGAP": 363,
  "averageHeartRate": 140,
  "maxHeartRate": 150,
  "minHeartRate": 118,
  "hrZones": { "Z1": 0, "Z2": 2580, "Z3": 2010, "Z4": 187, "Z5": 0 },
  "hrZonesPercentage": { "Z1": 0, "Z2": 56, "Z3": 43, "Z4": 4, "Z5": 0 },
  "averageCadence": 78,
  "maxCadence": 84,
  "totalAscent": 36,
  "totalDescent": 34,
  "laps": [
    {
      "lapNumber": 1,
      "lapName": "Warmup",
      "intensity": "warmup",
      "averagePace": 420,
      "averageGAP": 420,
      "averageHeartRate": 130
    }
  ],
  "calculatedAt": "2025-01-01T10:32:01.123Z"
}
```

> **Pace** is expressed in seconds per kilometre (lower = faster).
> **GAP** (Grade Adjusted Pace) normalises pace for elevation gain using a 10 % gradient factor.
> **minHeartRate** is derived from raw RECORD samples, not from the SESSION summary.

## Quickstart

```bash
# 1. Start infrastructure
docker compose up kafka postgres -d

# 2. Run the service
cd metrics-engine && mvn spring-boot:run
```

The service starts listening on `activities.normalized` automatically.
Use the full pipeline (`activity-service` → `activity-normalizer`) to produce an input event,
or publish directly to the topic:

```bash
docker exec -it kafka kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic activities.normalized
```