package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityMetricsCalculatorTest {

    private ActivityMetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        LapMetricsCalculator lapMetricsCalculator = new LapMetricsCalculator();
        calculator = new ActivityMetricsCalculator(lapMetricsCalculator, Clock.systemDefaultZone());
    }

    @Test
    void shouldCalculateAveragePace() {
        // Given
        int duration = 4777;
        BigDecimal distance = new BigDecimal("13138.37");

        // When
        Integer pace = calculator.calculatePace(duration, distance);

        // Then
        assertEquals(364, pace);
    }

    @Test
    void shouldReturnNullWhenDistanceIsZero() {
        // Given
        int duration = 1000;
        BigDecimal distance = BigDecimal.ZERO;

        // When
        Integer pace = calculator.calculatePace(duration, distance);

        // Then
        assertNull(pace);
    }

    @Test
    void shouldCalculateHrZonesPercentage() {
        // Given
        Map<String, Integer> hrZones = Map.of(
                "Z1", 0,
                "Z2", 2580,
                "Z3", 2010,
                "Z4", 0,
                "Z5", 0
        );
        int totalDuration = 4590;

        // When
        Map<String, Integer> percentages = calculator.calculateHrZonesPercentage(hrZones, totalDuration);

        // Then
        assertNotNull(percentages);
        assertEquals(5, percentages.size());
        assertEquals(0, percentages.get("Z1"));
        assertEquals(56, percentages.get("Z2"));  // 2580/4590 * 100 = 56.2% → 56
        assertEquals(43, percentages.get("Z3"));  // 2010/4590 * 100 = 43.8% → 43
        assertEquals(0, percentages.get("Z4"));
        assertEquals(0, percentages.get("Z5"));
    }

    @Test
    void shouldReturnEmptyMapWhenHrZonesIsNull() {
        // Given
        Map<String, Integer> hrZones = null;
        int totalDuration = 4590;

        // When
        Map<String, Integer> percentages = calculator.calculateHrZonesPercentage(hrZones, totalDuration);

        // Then
        assertNotNull(percentages);
        assertTrue(percentages.isEmpty());
    }

    @Test
    void shouldReturnEmptyMapWhenTotalDurationIsZero() {
        // Given
        Map<String, Integer> hrZones = Map.of("Z1", 100, "Z2", 200);
        int totalDuration = 0;

        // When
        Map<String, Integer> percentages = calculator.calculateHrZonesPercentage(hrZones, totalDuration);

        // Then
        assertNotNull(percentages);
        assertTrue(percentages.isEmpty());
    }

    @Test
    void shouldCalculateMinHeartRateFromSamples() {
        // Given
        List<ActivityNormalizedDto.SampleData> samples = List.of(
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, 125, 150, 100.0, 3.5, 200, 100.0
                ),
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, 118, 152, 100.0, 3.6, 210, 200.0
                ),
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, 132, 151, 100.0, 3.7, 220, 300.0
                ),
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, 128, 153, 100.0, 3.5, 200, 400.0
                )
        );

        // When
        Integer minHr = calculator.calculateMinHeartRate(samples);

        // Then
        assertEquals(118, minHr);
    }

    @Test
    void shouldReturnNullWhenSamplesAreEmpty() {
        // Given
        List<ActivityNormalizedDto.SampleData> samples = List.of();

        // When
        Integer minHr = calculator.calculateMinHeartRate(samples);

        // Then
        assertNull(minHr);
    }

    @Test
    void shouldReturnNullWhenNoSampleHasHeartRate() {
        // Given
        List<ActivityNormalizedDto.SampleData> samples = List.of(
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, null, 150, 100.0, 3.5, 200, 100.0
                ),
                new ActivityNormalizedDto.SampleData(
                        Instant.now(), 40.0, 4.0, null, 152, 100.0, 3.6, 210, 200.0
                )
        );

        // When
        Integer minHr = calculator.calculateMinHeartRate(samples);

        // Then
        assertNull(minHr);
    }

    @Test
    void shouldCalculateGradeAdjustedPace() {
        // Given
        int pace = 364;  // 6:04 /km en llano
        int totalAscent = 120;  // metros subidos
        BigDecimal totalDistance = new BigDecimal("10000");  // 10 km

        // When
        Integer gap = calculator.calculateGAP(pace, totalAscent, totalDistance);

        // Then
        // GAP = 364 / (1 + (120/10000 * 10)) = 364 / 1.12 = 325 sec/km
        assertNotNull(gap);
        assertTrue(gap < pace, "GAP should be faster (lower) than regular pace when climbing");
        assertEquals(325, gap, 5);
    }

    @Test
    void shouldReturnSamePaceWhenNoAscent() {
        // Given
        int pace = 360;
        int totalAscent = 0;
        BigDecimal totalDistance = new BigDecimal("10000");

        // When
        Integer gap = calculator.calculateGAP(pace, totalAscent, totalDistance);

        // Then
        assertEquals(360, gap);  // Sin desnivel, GAP = pace
    }

    @Test
    void shouldReturnNullWhenPaceIsNull() {
        // Given
        Integer pace = null;
        int totalAscent = 100;
        BigDecimal totalDistance = new BigDecimal("10000");

        // When
        Integer gap = calculator.calculateGAP(pace, totalAscent, totalDistance);

        // Then
        assertNull(gap);
    }

    @Test
    void shouldCalculateMaxPaceFromLaps() {
        // Given
        List<LapMetricsDto> laps = List.of(
                new LapMetricsDto(
                        1, "Warmup", "warmup", Instant.now(),
                        new BigDecimal("1000"), 420, 70,
                        420, null, null, null, null,  // pace = 420 seg/km (7:00 /km)
                        130, 135, null, 75, 78,
                        null, null, null, null,
                        null, null, null, 0, 0
                ),
                new LapMetricsDto(
                        2, "Interval 2", "active", Instant.now(),
                        new BigDecimal("1000"), 300, 85,
                        300, null, null, null, null,  // pace = 300 sec/km (5:00/km) — fastest lap
                        145, 150, null, 82, 85,
                        null, null, null, null,
                        null, null, null, 2, 1
                ),
                new LapMetricsDto(
                        3, "Cooldown", "cooldown", Instant.now(),
                        new BigDecimal("800"), 360, 60,
                        450, null, null, null, null,  // pace = 450 seg/km (7:30 /km)
                        125, 130, null, 72, 75,
                        null, null, null, null,
                        null, null, null, 0, 2
                )
        );

        // When
        Integer maxPace = calculator.calculateMaxPaceFromLaps(laps);

        // Then
        assertEquals(300, maxPace);  // El lap más rápido tiene 300 seg/km
    }

    @Test
    void shouldReturnNullWhenNoLaps() {
        // Given
        List<LapMetricsDto> laps = List.of();

        // When
        Integer maxPace = calculator.calculateMaxPaceFromLaps(laps);

        // Then
        assertNull(maxPace);
    }

    @Test
    void shouldCalculateCompleteActivityMetrics() {
        // Given - Datos simulados de una actividad real
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
                Map.of("Z1", 0, "Z2", 2580, "Z3", 2010, "Z4", 187, "Z5", 0),  // timeInHrZones
                null,                          // timeInPowerZones
                195,                           // maxHeartRateConfig
                59,                            // restingHeartRate
                171,                           // thresholdHeartRate
                418                            // functionalThresholdPower
        );

        var lap1 = new ActivityNormalizedDto.LapData(
                1, now, new BigDecimal("1000"), 420, 420, 70,
                130, 135, 75, 78, new BigDecimal("2.4"), new BigDecimal("2.8"),
                null, null, null, null, null, null, null, 0, 0,
                "warmup", 0
        );

        var lap2 = new ActivityNormalizedDto.LapData(
                2, now, new BigDecimal("1000"), 300, 300, 85,
                145, 150, 82, 85, new BigDecimal("3.3"), new BigDecimal("3.8"),
                null, null, null, null, null, null, null, 2, 1,
                "active", 1
        );

        var samples = List.of(
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 125, 150, 100.0, 3.5, 200, 100.0),
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 118, 152, 100.0, 3.6, 210, 200.0),
                new ActivityNormalizedDto.SampleData(now, 40.0, 4.0, 155, 151, 100.0, 3.7, 220, 300.0)
        );

        var dto = new ActivityNormalizedDto(
                UUID.randomUUID(),
                "test-user",
                "Garmin Fenix 7",
                now,
                session,
                List.of(lap1, lap2),
                samples,
                now
        );

        // When
        ActivityMetricsDto metrics = calculator.calculate(dto);

        // Then
        assertNotNull(metrics);
        assertEquals(dto.activityId(), metrics.activityId());
        assertEquals(dto.userId(), metrics.userId());

        assertEquals(new BigDecimal("13138.37"), metrics.totalDistance());
        assertEquals(4777, metrics.totalDuration());
        assertEquals(1048, metrics.totalCalories());

        assertEquals(364, metrics.averagePace());
        assertNotNull(metrics.averageGAP());
        assertEquals(118, metrics.minHeartRate());

        assertNotNull(metrics.hrZones());
        assertEquals(5, metrics.hrZones().size());
        assertNotNull(metrics.hrZonesPercentage());
        assertEquals(54, metrics.hrZonesPercentage().get("Z2"));

        assertEquals(92.7, metrics.averageVerticalOscillation());
        assertEquals(291.8, metrics.averageStanceTime());

        assertEquals(3.5, metrics.trainingEffect());
        assertEquals(112.56, metrics.trainingLoadPeak());

        assertNotNull(metrics.calculatedAt());
        assertTrue(metrics.calculatedAt().isAfter(now.minusSeconds(5)));

        assertEquals(2, metrics.laps().size());
        assertEquals("Warmup", metrics.laps().get(0).lapName());
        assertEquals("Interval 2", metrics.laps().get(1).lapName());
        assertEquals(300, metrics.maxPace());
    }
}