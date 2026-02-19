# Activity Normalizer

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-6DB33F?logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.3-231F20?logo=apachekafka&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-4169E1?logo=postgresql&logoColor=white)

Consumes raw activity data from Kafka, normalizes it into structured records, persists to PostgreSQL, and publishes downstream events to `activities.normalized`.

## Data Flow

1. Consumes raw JSON payload from `activities.raw.ingested`
2. Extracts userId, device, timestamp, duration, distance and GPS/HR samples
3. Persists normalized activity to PostgreSQL (`activities` table)
4. Publishes structured event to `activities.normalized`

## Kafka

| Direction | Topic | Partition key |
|-----------|-------|---------------|
| Consumes | `activities.raw.ingested` | — |
| Produces | `activities.normalized` | `userId` |

### Input — `activities.raw.ingested`

```json
{
  "userId": "user-12345",
  "device": "Garmin-Fenix-7-Pro",
  "timestamp": "2025-01-01T10:30:00Z",
  "raw": {
    "duration_s": 2780,
    "distance_m": 10042.5,
    "samples": [
      {
        "ts": "2025-01-01T10:30:00Z",
        "lat": 40.416775,
        "lon": -3.703790,
        "hr": 145,
        "pace": 277,
        "altitude": 650.5,
        "cadence": 172
      }
    ]
  }
}
```

### Output — `activities.normalized`

```json
{
  "activityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "userId": "user-12345",
  "device": "Garmin-Fenix-7-Pro",
  "startedAt": "2025-01-01T10:30:00Z",
  "durationSeconds": 2780,
  "distanceMeters": 10042.50,
  "samples": [
    {
      "timestamp": "2025-01-01T10:30:00Z",
      "latitude": 40.416775,
      "longitude": -3.703790,
      "heartRate": 145,
      "paceSecondsPerKm": 277,
      "altitude": 650.5,
      "cadence": 172
    }
  ],
  "normalizedAt": "2025-01-01T10:30:05.123Z"
}
```

## Quickstart

```bash
# 1. Start infrastructure
docker compose up kafka postgres -d

# 2. Run the service
cd activity-normalizer && mvn spring-boot:run
```

The service starts listening on `activities.raw.ingested` automatically.
Use the `activity-service` to publish a test payload — this service will pick it up and publish to `activities.normalized`.