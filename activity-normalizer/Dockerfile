# ═══════════════════════════════════════════════════════
# Stage 1: Build con Maven
# ═══════════════════════════════════════════════════════
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar solo pom.xml primero (para cachear dependencias)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ═══════════════════════════════════════════════════════
# Stage 2: Runtime (imagen final pequeña)
# ═══════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR desde el stage de build
COPY --from=build /app/target/activity-normalizer-*.jar app.jar

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]