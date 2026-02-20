package com.runalytics.metrics_engine.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import com.runalytics.metrics_engine.entity.LapMetrics;
import com.runalytics.metrics_engine.kafka.MetricsConsumer;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        partitions = 1,
        topics = {"activities.normalized", "activities.metrics.calculated"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9093",
                "port=9093"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetricsEngineIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(MetricsEngineIntegrationTest.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");

        // Kafka (embedded)
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9093");
    }

    @Autowired
    private KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Autowired
    private ActivityMetricsRepository activityRepository;

    @Autowired
    private LapMetricsRepository lapRepository;

    @BeforeEach
    void setUp() {
        activityRepository.deleteAll();
        lapRepository.deleteAll();
    }

    @Test
    void shouldProcessActivityFromKafkaToDatabase() throws Exception {
        // Given - Actividad normalizada simulada
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("13138.37"),   // totalDistance
                4777,                          // totalTimerTime
                4987,                          // totalElapsedTime
                1048,                          // totalCalories
                140,                           // avgHeartRate
                150,                           // maxHeartRate
                78,                            // avgCadence
                84,                            // maxCadence
                new BigDecimal("2.75"),        // enhancedAvgSpeed
                new BigDecimal("3.984"),       // enhancedMaxSpeed
                349,                           // avgPower
                569,                           // maxPower
                351,                           // normalizedPower
                92.7,                          // avgVerticalOscillation
                291.8,                         // avgStanceTime
                8.82,                          // avgVerticalRatio
                1052,                          // avgStepLength
                36,                            // totalAscent
                34,                            // totalDescent
                3.5,                           // totalTrainingEffect
                0.0,                           // totalAnaerobicTrainingEffect
                112.56,                        // trainingLoadPeak
                75,                            // workoutFeel
                60,                            // workoutRpe
                Map.of("Z1", 0, "Z2", 2580, "Z3", 2010, "Z4", 187, "Z5", 0),
                null,
                195, 59, 171, 418
        );

        var lap1 = new ActivityNormalizedDto.LapData(
                1, now, new BigDecimal("1000"), 420, 420, 70,
                130, 135, 75, 78,
                new BigDecimal("2.4"), new BigDecimal("2.8"),
                null, null, null, null, null, null, null,
                0, 0, "warmup", 0
        );

        var lap2 = new ActivityNormalizedDto.LapData(
                2, now, new BigDecimal("1000"), 300, 300, 85,
                145, 150, 82, 85,
                new BigDecimal("3.3"), new BigDecimal("3.8"),
                null, null, null, null, null, null, null,
                2, 1, "active", 1
        );

        var samples = List.of(
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 125, 150, 100.0, 3.5, 200, 100.0),
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 118, 152, 100.0, 3.6, 210, 200.0),
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 155, 151, 100.0, 3.7, 220, 300.0)
        );

        var normalizedActivity = new ActivityNormalizedDto(
                activityId,
                "integration-test-user",
                "Garmin Fenix 7",
                now,
                session,
                List.of(lap1, lap2),
                samples,
                now
        );

        // When - Publicar a Kafka
        kafkaTemplate.send("activities.normalized", activityId.toString(), normalizedActivity);

        // Then - Esperar a que se procese (máximo 10 segundos)
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    // Verificar que se guardó en BD
                    assertTrue(activityRepository.existsByActivityId(activityId),
                            "ActivityMetrics should be saved in database");
                });


        // Verificar métricas de actividad
        ActivityMetrics savedMetrics = activityRepository.findByActivityId(activityId)
                .orElseThrow(() -> new AssertionError("ActivityMetrics not found"));

        assertEquals("integration-test-user", savedMetrics.getUserId());
        assertEquals(0, new BigDecimal("13138.37").compareTo(savedMetrics.getTotalDistance()));
        assertEquals(4777, savedMetrics.getTotalDuration());
        assertEquals(1048, savedMetrics.getTotalCalories());

        // Verificar métricas calculadas
        assertEquals(364, savedMetrics.getAveragePace());  // Calculado
        assertEquals(300, savedMetrics.getMaxPace());       // Calculado desde laps
        assertEquals(118, savedMetrics.getMinHeartRate());  // Calculado desde samples
        assertNotNull(savedMetrics.getAverageGAP());        // Calculado

        // Verificar HR zones
        assertNotNull(savedMetrics.getHrZones());
        assertEquals(5, savedMetrics.getHrZones().size());
        assertNotNull(savedMetrics.getHrZonesPercentage());
        assertTrue(savedMetrics.getHrZonesPercentage().get("Z2") > 50);

        // Verificar timestamps
        assertNotNull(savedMetrics.getCreatedAt());
        assertNotNull(savedMetrics.getUpdatedAt());
        assertNotNull(savedMetrics.getCalculatedAt());

        // Verificar laps
        List<LapMetrics> savedLaps = lapRepository.findByActivityIdOrderByLapNumberAsc(activityId);
        assertEquals(2, savedLaps.size());

        // Verificar lap 1 (warmup)
        LapMetrics lap1Saved = savedLaps.get(0);
        assertEquals(1, lap1Saved.getLapNumber());
        assertEquals("Warmup", lap1Saved.getLapName());
        assertEquals("warmup", lap1Saved.getIntensity());
        assertEquals(0, new BigDecimal("1000").compareTo(lap1Saved.getDistance()));
        assertEquals(420, lap1Saved.getDuration());
        assertEquals(420, lap1Saved.getAveragePace());

        // Verificar lap 2 (active)
        LapMetrics lap2Saved = savedLaps.get(1);
        assertEquals(2, lap2Saved.getLapNumber());
        assertEquals("Interval 2", lap2Saved.getLapName());
        assertEquals("active", lap2Saved.getIntensity());
        assertEquals(300, lap2Saved.getAveragePace());  // El lap más rápido
    }
}