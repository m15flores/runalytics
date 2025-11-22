# Runalytics

Microservicio de ingestión y análisis de actividades deportivas.

---

## Requisitos

- Docker >= 24
- Docker Compose >= 2.0
- Maven >= 3.9
- Java 17

---

## Estructura del proyecto

```text
runalytics/
├── activity-service/    # Microservicio Spring Boot
├── infrastructure/     # Docker Compose y redes
├── README.md
└── .gitignore
```

---

## Levantar infraestructura y microservicio

Desde la carpeta `infrastructure/`:

```bash
docker compose up -d
```
