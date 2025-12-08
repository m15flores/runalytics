package com.runalitycs.metrics_engine.integration;

import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.repository.ActivityMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        topics = {"activities.normalized", "activities.metrics.calculated", "activities.normalized.dlq"},
        partitions = 1
)
class MetricsEngineErrorHandlingTest {

    private static final Logger log = LoggerFactory.getLogger(MetricsEngineErrorHandlingTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.kafka.bootstrap-servers",
                () -> "${spring.embedded.kafka.brokers}");
    }

    @Autowired
    private KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Autowired
    private ActivityMetricsRepository activityRepository;

    // ─────────────────────────────────────────────────────────────
    // TEST 1: Activity con datos inválidos (null userId)
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldHandleInvalidActivity_NullUserId() throws Exception {
        // Given: Activity con userId = null
        UUID activityId = UUID.randomUUID();
        var sessionData = createValidSessionData();

        var invalidActivity = new ActivityNormalizedDto(
                activityId,
                null, // ← userId inválido
                "Garmin",
                Instant.now(),
                sessionData,
                List.of(),
                List.of(),
                Instant.now()
        );

        // When: Enviar mensaje inválido
        kafkaTemplate.send("activities.normalized", activityId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        // Then: NO debe guardarse en BD (validación falla)
        await()
                .pollDelay(2, TimeUnit.SECONDS) // Esperar un poco
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean exists = activityRepository.existsByActivityId(activityId);
                    assertFalse(exists, "Invalid activity should NOT be saved");
                });

        log.info("TEST PASSED: Invalid activity correctly rejected");
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 2: Activity con sessionData = null
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldHandleInvalidActivity_NullSessionData() throws Exception {
        // Given: Activity con sessionData = null
        UUID activityId = UUID.randomUUID();

        var invalidActivity = new ActivityNormalizedDto(
                activityId,
                "test-user",
                "Garmin",
                Instant.now(),
                null, // ← sessionData inválido
                List.of(),
                List.of(),
                Instant.now()
        );

        // When: Enviar mensaje inválido
        kafkaTemplate.send("activities.normalized", activityId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        // Then: NO debe guardarse en BD
        await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean exists = activityRepository.existsByActivityId(activityId);
                    assertFalse(exists, "Activity without sessionData should NOT be saved");
                });

        log.info("TEST PASSED: Activity without sessionData correctly rejected");
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 3: Activity válida después de mensaje inválido (recovery)
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldRecoverAfterInvalidMessage() throws Exception {
        // Given: 1 mensaje inválido + 1 mensaje válido
        UUID invalidId = UUID.randomUUID();
        UUID validId = UUID.randomUUID();

        var invalidActivity = new ActivityNormalizedDto(
                invalidId, null, "Garmin", Instant.now(),
                null, List.of(), List.of(), Instant.now()
        );

        var validActivity = new ActivityNormalizedDto(
                validId, "test-user", "Garmin", Instant.now(),
                createValidSessionData(), List.of(), List.of(), Instant.now()
        );

        // When: Enviar primero el inválido, luego el válido
        kafkaTemplate.send("activities.normalized", invalidId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        Thread.sleep(1000); // Dar tiempo a que falle

        kafkaTemplate.send("activities.normalized", validId.toString(), validActivity)
                .get(10, TimeUnit.SECONDS);

        // Then: El válido SÍ debe guardarse
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    boolean invalidExists = activityRepository.existsByActivityId(invalidId);
                    boolean validExists = activityRepository.existsByActivityId(validId);

                    assertFalse(invalidExists, "Invalid activity should NOT be saved");
                    assertTrue(validExists, "Valid activity SHOULD be saved");
                });

        log.info("TEST PASSED: Consumer recovered after invalid message");
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER: Crear sessionData válida
    // ─────────────────────────────────────────────────────────────
    private ActivityNormalizedDto.SessionData createValidSessionData() {
        return new ActivityNormalizedDto.SessionData(
                new BigDecimal("1000"), // totalDistance
                300, // totalElapsedTime
                300, // totalTimerTime
                100, // totalAscent
                140, // avgHeartRate
                150, // maxHeartRate
                78, // avgRunningCadence
                84, // maxRunningCadence
                new BigDecimal("5.00"), // avgSpeed (m/s)
                new BigDecimal("3.33"), // avgPace (min/km)
                null, null, null, null, null, null, null,
                0, 0, null, null, null, null, null,
                Map.of("Z1", 0, "Z2", 300),
                null, 195, 59, 171, 418
        );
    }
}