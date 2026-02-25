package com.runalytics.metrics_engine.integration;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
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
        topics = {"activities.normalized", "metrics.calculated"},
        partitions = 1
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MetricsEngineComplexTest {

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
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
    }

    @Autowired
    private KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Autowired
    private ActivityMetricsRepository activityRepository;

    @Autowired
    private LapMetricsRepository lapRepository;

    // ═════════════════════════════════════════════════════════════
    // TEST 1: Activity without Heart Rate (sensor not used)
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldHandleActivityWithoutHeartRate() throws Exception {
        // Given: activity with no HR sensor
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10000"), // 10km
                3600, 3600, 500,
                null, null, // avgHeartRate = null, maxHeartRate = null
                160, 180, // cadence OK
                new BigDecimal("2.78"), new BigDecimal("4.17"),
                null, null, null, null, null, null, null,
                100, 80, null, null, null, null, null,
                null, // timeInHrZones = null (no HR data)
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", Instant.now(),
                session, List.of(), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: Debe guardarse, pero HR metrics = null
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow(() -> new AssertionError("Activity should be saved"));

                    assertNull(metrics.getAverageHeartRate(), "Avg HR should be null");
                    assertNull(metrics.getMaxHeartRate(), "Max HR should be null");
                    assertEquals(Map.of(), metrics.getHrZones(), "HR zones should be empty map");
                    assertEquals(Map.of(), metrics.getHrZonesPercentage(), "HR zones % should be empty map");

                    assertNotNull(metrics.getAveragePace(), "Pace should exist");
                    assertNotNull(metrics.getAverageCadence(), "Cadence should exist");
                    assertEquals(0, new BigDecimal("10000").compareTo(metrics.getTotalDistance()),
                            "Total distance should be 10000");

                    log.info("Activity without HR processed correctly");
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 2: Activity without GPS (indoor treadmill)
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldHandleIndoorTreadmillActivity() throws Exception {
        // Given: indoor treadmill activity (no GPS)
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("5000"), // 5km
                1800, 1800, 300,
                145, 162,
                165, 175,
                new BigDecimal("2.78"), new BigDecimal("3.33"),
                null, null, null, null, null, null, null,
                0, 0, // totalAscent = 0, totalDescent = 0 (flat treadmill)
                null, null, null, null, null,
                Map.of("Z1", 0, "Z2", 1800, "Z3", 0, "Z4", 0, "Z5", 0),
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", Instant.now(),
                session, List.of(), List.of(),
                Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: GAP must equal pace (no elevation data)
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow();

                    assertNotNull(metrics.getAveragePace());

                    if (metrics.getAverageGAP() != null) {
                        // With zero elevation GAP must be approximately equal to pace
                        int difference = Math.abs(metrics.getAveragePace() - metrics.getAverageGAP());
                        assertTrue(difference <= 5,
                                "GAP should be similar to pace on flat terrain (diff: " + difference + "s)");
                    }

                    log.info("Indoor treadmill activity processed correctly");
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 3: HR zones with realistic distribution
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldCalculateHeartRateZonesCorrectly() throws Exception {
        // Given: activity with Z2 (60%) + Z3 (40%) distribution
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10000"),
                3600, 3600, 500,
                150, 172, // avg/max HR
                162, 180,
                new BigDecimal("2.78"), new BigDecimal("4.17"),
                null, null, null, null, null, null, null,
                100, 80, null, null, null, null, null,
                Map.of(
                        "Z1", 0,
                        "Z2", 2160,  // 60% del tiempo (2160s = 36min)
                        "Z3", 1440,  // 40% del tiempo (1440s = 24min)
                        "Z4", 0,
                        "Z5", 0
                ),
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", Instant.now(),
                session, List.of(), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: percentages must be calculated correctly
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow();

                    assertEquals(150, metrics.getAverageHeartRate());
                    assertEquals(172, metrics.getMaxHeartRate());

                    Map<String, Integer> hrZones = metrics.getHrZones();
                    assertNotNull(hrZones);
                    assertEquals(0, hrZones.get("Z1"));
                    assertEquals(2160, hrZones.get("Z2"));
                    assertEquals(1440, hrZones.get("Z3"));
                    assertEquals(0, hrZones.get("Z4"));
                    assertEquals(0, hrZones.get("Z5"));

                    Map<String, Integer> hrZonesPercentage = metrics.getHrZonesPercentage();
                    assertNotNull(hrZonesPercentage);
                    assertEquals(0, hrZonesPercentage.get("Z1"));
                    assertEquals(60, hrZonesPercentage.get("Z2"));
                    assertEquals(40, hrZonesPercentage.get("Z3"));
                    assertEquals(0, hrZonesPercentage.get("Z4"));
                    assertEquals(0, hrZonesPercentage.get("Z5"));

                    log.info("HR zones calculated correctly: Z2=60%, Z3=40%");
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 4: Inconsistent cadence (walk breaks)
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldHandleInconsistentCadence() throws Exception {
        // Given: run/walk with variable cadence
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("8000"),
                3600, 3600, 400,
                140, 165,
                145, 180, // avg cadence is low (many walk pauses)
                new BigDecimal("2.22"), new BigDecimal("3.33"),
                null, null, null, null, null, null, null,
                50, 40, null, null, null, null, null,
                Map.of("Z1", 1800, "Z2", 1800, "Z3", 0, "Z4", 0, "Z5", 0),
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", Instant.now(),
                session, List.of(), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: Debe procesarse normalmente
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow();

                    assertEquals(145, metrics.getAverageCadence());
                    assertEquals(180, metrics.getMaxCadence());

                    // Pace is slower due to walk breaks
                    assertTrue(metrics.getAveragePace() > 360,
                            "Pace should be slower than 6:00/km due to walk breaks (actual: "
                                    + metrics.getAveragePace() + "s/km)");

                    log.info("Run/walk activity processed correctly");
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 5: Lap metrics per lap
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldCalculateLapMetricsCorrectly() throws Exception {
        // Given: activity with 3 laps (warmup, main effort, cooldown)
        UUID activityId = UUID.randomUUID();
        Instant startTime = Instant.now();

        var lap1 = new ActivityNormalizedDto.LapData(
                1, startTime,
                new BigDecimal("2000"), 720, 720, 120,
                135, 145, 155, 165,
                new BigDecimal("2.78"), new BigDecimal("3.05"),
                null, null, null, null, null, null, null,
                20, 15, "warmup", 1
        );

        var lap2 = new ActivityNormalizedDto.LapData(
                2, startTime.plusSeconds(720),
                new BigDecimal("5000"), 1500, 1500, 300,
                165, 178, 170, 185,
                new BigDecimal("3.33"), new BigDecimal("3.89"),
                null, null, null, null, null, null, null,
                60, 50, "active", 2
        );

        var lap3 = new ActivityNormalizedDto.LapData(
                3, startTime.plusSeconds(2220),
                new BigDecimal("3000"), 1080, 1080, 150,
                140, 155, 158, 170,
                new BigDecimal("2.78"), new BigDecimal("3.20"),
                null, null, null, null, null, null, null,
                20, 18, "cooldown", 3
        );

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10000"), 3300, 3300, 570,
                150, 178, 165, 185,
                new BigDecimal("3.03"), new BigDecimal("3.89"),
                null, null, null, null, null, null, null,
                100, 83, null, null, null, null, null,
                Map.of("Z1", 0, "Z2", 1800, "Z3", 1500, "Z4", 0, "Z5", 0),
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", startTime,
                session, List.of(lap1, lap2, lap3), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: 3 laps must be saved with correct metrics
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var laps = lapRepository.findByActivityIdOrderByLapNumberAsc(activityId);

                    assertEquals(3, laps.size(), "Should have 3 laps");

                    var savedLap1 = laps.get(0);
                    assertEquals(1, savedLap1.getLapNumber());
                    assertEquals(135, savedLap1.getAverageHeartRate());
                    assertEquals("warmup", savedLap1.getIntensity());

                    var savedLap2 = laps.get(1);
                    assertEquals(2, savedLap2.getLapNumber());
                    assertEquals(165, savedLap2.getAverageHeartRate());
                    assertEquals("active", savedLap2.getIntensity());
                    assertTrue(savedLap2.getAveragePace().compareTo(savedLap1.getAveragePace()) < 0,
                            "Lap 2 should be faster than lap 1");

                    var savedLap3 = laps.get(2);
                    assertEquals(3, savedLap3.getLapNumber());
                    assertEquals("cooldown", savedLap3.getIntensity());

                    log.info("All 3 laps processed correctly");
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 6: GAP on mountain terrain
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldCalculateGAPOnMountainTerrain() throws Exception {
        // Given: mountain run with significant elevation gain
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10000"),
                4200, 4200, 600, // 70min (slower due to elevation)
                155, 175,
                158, 180,
                new BigDecimal("2.38"), new BigDecimal("3.33"), // avg 6 km/h
                null, null, null, null, null, null, null,
                450, 420, // significant ascent/descent
                null, null, null, null, null,
                Map.of("Z1", 0, "Z2", 1800, "Z3", 2400, "Z4", 0, "Z5", 0),
                null, 195, 59, 171, null
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Garmin", Instant.now(),
                session, List.of(), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: GAP must be lower (faster) than actual pace on uphill terrain
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow();

                    Integer avgPace = metrics.getAveragePace();
                    Integer gap = metrics.getAverageGAP();

                    assertNotNull(avgPace, "Avg pace should exist");

                    assertEquals(450, metrics.getTotalAscent());
                    assertEquals(420, metrics.getTotalDescent());

                    if (gap != null) {
                        assertTrue(gap.compareTo(avgPace) < 0,
                                "GAP should be faster than actual pace on uphill");
                        log.info("GAP={} Pace={}", gap, avgPace);
                    }
                });
    }

    // ═════════════════════════════════════════════════════════════
    // TEST 7: Activity with Power Meter
    // ═════════════════════════════════════════════════════════════
    @Test
    void shouldHandleActivityWithPowerMeter() throws Exception {
        // Given: running activity with power meter (Stryd)
        UUID activityId = UUID.randomUUID();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10000"),
                3600, 3600, 500,
                150, 172,
                162, 180,
                new BigDecimal("2.78"), new BigDecimal("4.17"),
                245, 320, 260,
                null, null, null, null,
                100, 80,
                3.2, 2.5, 45.0,
                null, null,
                Map.of("Z1", 0, "Z2", 2160, "Z3", 1440, "Z4", 0, "Z5", 0),
                null, 195, 59, 171, 300
        );

        var activity = new ActivityNormalizedDto(
                activityId, "test-user", "Stryd", Instant.now(),
                session, List.of(), List.of(), Instant.now()
        );

        // When
        kafkaTemplate.send("activities.normalized", activityId.toString(), activity)
                .get(10, TimeUnit.SECONDS);

        // Then: Power metrics deben guardarse
        await()
                .atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var metrics = activityRepository.findByActivityId(activityId)
                            .orElseThrow();

                    assertEquals(245, metrics.getAveragePower());
                    assertEquals(320, metrics.getMaxPower());
                    assertEquals(260, metrics.getNormalizedPower());

                    assertNotNull(metrics.getTrainingLoadPeak());
                    assertTrue(metrics.getTrainingEffect() > 0);

                    log.info("Power metrics processed correctly");
                });
    }
}