# Runalytics

![CI - activity-service](https://github.com/m15flores/runalytics/actions/workflows/activity-service.yml/badge.svg)
![CI - activity-normalizer](https://github.com/m15flores/runalytics/actions/workflows/activity-normalizer.yml/badge.svg)
![CI - metrics-engine](https://github.com/m15flores/runalytics/actions/workflows/metrics-engine.yml/badge.svg)
![CI - report-generator](https://github.com/m15flores/runalytics/actions/workflows/report-generator.yml/badge.svg)
![CI - ai-coach](https://github.com/m15flores/runalytics/actions/workflows/ai-coach.yml/badge.svg)

AI-powered running training analysis platform. Parses FIT files from Garmin devices, calculates training metrics, generates weekly reports, and produces coaching recommendations using GPT-4o.

Fully event-driven — all communication between services happens through Apache Kafka.

---

## Requirements

- Docker >= 24
- Docker Compose >= 2.0
- Java 17
- Maven >= 3.9

---

## Project structure

```text
runalytics/
├── activity-service/       # Ingests raw FIT files via REST, publishes to Kafka
├── activity-normalizer/    # Parses FIT files, extracts structured data
├── metrics-engine/         # Calculates training metrics (pace, HR zones, cadence)
├── report-generator/       # Generates weekly markdown training reports
├── ai-coach/               # Generates AI coaching recommendations via GPT-4o
├── docker-compose.yml
└── README.md
```

---

## Run locally

```bash
docker compose up
```

Requires an `.env` file at the root with `OPENAI_API_KEY`. See `.env.example`.