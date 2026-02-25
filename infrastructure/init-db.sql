-- PostgreSQL extensions required by Runalytics services
-- DDL (tables) is managed by Hibernate (ddl-auto: update) at service startup

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;