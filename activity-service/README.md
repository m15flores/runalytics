# Activity Service

![Java](https://img.shields.io/badge/Java-17-007396?logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.8-6DB33F?logo=springboot&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.3-231F20?logo=apachekafka&logoColor=white)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248?logo=mongodb&logoColor=white)

Entry point for raw activity data in the Runalytics platform. Receives fitness activity payloads via REST, validates them, and publishes them to Kafka for downstream processing by the `activity-normalizer`.

## API

### `POST /activities`

Ingests a raw activity and publishes it to Kafka.

**Request**

```json
{
  "userId": "user-12345",
  "device": "Garmin-Fenix-7-Pro",
  "timestamp": "2025-01-01T10:30:00Z",
  "source": "garmin",
  "raw": {
    "distance_m": 10042,
    "duration_s": 2780
  }
}
```

**Responses**

| Status | Description |
|--------|-------------|
| `201 Created` | Activity accepted — `{ "userId": "user-12345" }` |
| `400 Bad Request` | Validation error or malformed JSON — includes `fieldErrors` |
| `500 Internal Server Error` | Unexpected failure (e.g. Kafka unavailable) |

## Kafka

| Direction | Topic | Partition key |
|-----------|-------|---------------|
| Produces | `activities.raw.ingested` | `userId` |

### Published event — `activities.raw.ingested`

The published message is the request body verbatim. Consumed by `activity-normalizer`.

```json
{
  "userId": "user-12345",
  "device": "Garmin-Fenix-7-Pro",
  "timestamp": "2025-01-01T10:30:00Z",
  "source": "garmin",
  "raw": {
    "distance_m": 10042,
    "duration_s": 2780
  }
}
```

## Quickstart

```bash
# 1. Start infrastructure
docker compose up kafka mongodb -d

# 2. Run the service (port 8082)
cd activity-service && mvn spring-boot:run

# 3. Send a test activity
curl -s -X POST http://localhost:8082/activities \
  -H "Content-Type: application/json" \
  -d '{"userId":"user-12345","timestamp":"2025-01-01T10:30:00Z","raw":{"distance_m":10042}}' \
  | jq .
```