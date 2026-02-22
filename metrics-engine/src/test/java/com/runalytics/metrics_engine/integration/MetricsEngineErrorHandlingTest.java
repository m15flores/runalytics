package com.runalytics.metrics_engine.integration;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
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


@Slf4j
@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        topics = {"activities.normalized", "activities.metrics.calculated", "activities.normalized.dlq"},
        partitions = 1
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetricsEngineErrorHandlingTest {

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
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Autowired
    private KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Autowired
    private ActivityMetricsRepository activityRepository;

    // ─────────────────────────────────────────────────────────────
    // TEST 1: Invalid activity — null userId
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldHandleInvalidActivity_NullUserId() throws Exception {
        // Given: activity with null userId
        UUID activityId = UUID.randomUUID();
        var sessionData = createValidSessionData();

        var invalidActivity = new ActivityNormalizedDto(
                activityId,
                null, // userId = null (invalid)
                "Garmin",
                Instant.now(),
                sessionData,
                List.of(),
                List.of(),
                Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        // Then: must not be saved (validation fails, message is acknowledged without retry)
        await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(activityRepository.existsByActivityId(activityId),
                        "Invalid activity should NOT be saved"));

        log.info("Invalid activity correctly rejected");
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 2: Invalid activity — null sessionData
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldHandleInvalidActivity_NullSessionData() throws Exception {
        // Given: activity with null sessionData
        UUID activityId = UUID.randomUUID();

        var invalidActivity = new ActivityNormalizedDto(
                activityId,
                "test-user",
                "Garmin",
                Instant.now(),
                null, // sessionData = null (invalid)
                List.of(),
                List.of(),
                Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        // Then
        await()
                .pollDelay(2, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(activityRepository.existsByActivityId(activityId),
                        "Activity without sessionData should NOT be saved"));

        log.info("Activity without sessionData correctly rejected");
    }

    // ─────────────────────────────────────────────────────────────
    // TEST 3: Valid activity processed after invalid message (recovery)
    // ─────────────────────────────────────────────────────────────
    @Test
    void shouldRecoverAfterInvalidMessage() throws Exception {
        // Given: 1 invalid message followed by 1 valid message
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

        // When: send invalid first, then valid — let invalid be rejected before the valid arrives
        kafkaTemplate.send("activities.normalized", invalidId.toString(), invalidActivity)
                .get(10, TimeUnit.SECONDS);

        await()
                .pollDelay(1, TimeUnit.SECONDS)
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> assertFalse(activityRepository.existsByActivityId(invalidId)));

        kafkaTemplate.send("activities.normalized", validId.toString(), validActivity)
                .get(10, TimeUnit.SECONDS);

        // Then: valid activity must be saved; invalid must not
        await()
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertFalse(activityRepository.existsByActivityId(invalidId),
                            "Invalid activity should NOT be saved");
                    assertTrue(activityRepository.existsByActivityId(validId),
                            "Valid activity SHOULD be saved");
                });

        log.info("Consumer recovered after invalid message");
    }

    // ─────────────────────────────────────────────────────────────
    // HELPER
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