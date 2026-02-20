package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class LapMetricsCalculatorTest {

    private LapMetricsCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new LapMetricsCalculator();
    }

    @Test
    void shouldCalculateLapPace() {
        // Given
        int duration = 370;  // segundos
        BigDecimal distance = new BigDecimal("1000");  // metros

        // When
        Integer pace = calculator.calculateLapPace(duration, distance);

        // Then
        assertEquals(370, pace);  // 370 seg/km = 6:10 /km
    }

    @Test
    void shouldCalculateLapGAP() {
        // Given
        int pace = 370;
        int totalAscent = 5;
        BigDecimal distance = new BigDecimal("1000");

        // When
        Integer gap = calculator.calculateLapGAP(pace, totalAscent, distance);

        // Then
        // GAP = 370 / (1 + (5/1000 * 10)) = 370 / 1.05 = 352
        assertNotNull(gap);
        assertTrue(gap < pace);
    }

    @Test
    void shouldCalculateCompleteLapMetrics() {
        // Given
        Instant now = Instant.now();

        var lapData = new ActivityNormalizedDto.LapData(
                1,                              // lapNumber
                now,                            // startTime
                new BigDecimal("1000"),         // totalDistance
                370,                            // totalTimerTime
                375,                            // totalElapsedTime
                82,                             // totalCalories
                137,                            // avgHeartRate
                141,                            // maxHeartRate
                77,                             // avgCadence
                79,                             // maxCadence
                new BigDecimal("2.7"),          // enhancedAvgSpeed
                new BigDecimal("3.06"),         // enhancedMaxSpeed
                335,                            // avgPower
                419,                            // maxPower
                335,                            // normalizedPower
                93.1,                           // avgVerticalOscillation
                297.8,                          // avgStanceTime
                8.93,                           // avgVerticalRatio
                1042,                           // avgStepLength
                1,                              // totalAscent
                3,                              // totalDescent
                "active",                       // intensity
                1                               // wktStepIndex
        );

        // When
        LapMetricsDto metrics = calculator.calculate(lapData);

        // Then
        assertNotNull(metrics);
        assertEquals(1, metrics.lapNumber());
        assertEquals("active", metrics.intensity());
        assertEquals(370, metrics.averagePace());
        assertNotNull(metrics.averageGAP());
        assertEquals(new BigDecimal("1000"), metrics.distance());
        assertEquals(137, metrics.averageHeartRate());
        assertEquals(93.1, metrics.averageVerticalOscillation());
    }

    @Test
    void shouldGenerateLapNameForWarmup() {
        // Given
        var lapData = new ActivityNormalizedDto.LapData(
                1, Instant.now(), new BigDecimal("800"), 300, 300, 66,
                132, 139, 78, 81, new BigDecimal("2.8"), new BigDecimal("3.5"),
                340, 469, 341, 93.7, 289.2, 8.63, 1088,
                0, 6, "warmup", 0
        );

        // When
        LapMetricsDto metrics = calculator.calculate(lapData);

        // Then
        assertEquals("Warmup", metrics.lapName());
    }

    @Test
    void shouldGenerateLapNameForCooldown() {
        // Given
        var lapData = new ActivityNormalizedDto.LapData(
                15, Instant.now(), new BigDecimal("1090"), 357, 357, 66,
                139, 144, 156, 160, new BigDecimal("3.0"), new BigDecimal("3.2"),
                null, null, null, null, null, null, null,
                0, 0, "cooldown", 2
        );

        // When
        LapMetricsDto metrics = calculator.calculate(lapData);

        // Then
        assertEquals("Cooldown", metrics.lapName());
    }

    @Test
    void shouldGenerateLapNameForActiveInterval() {
        // Given
        var lapData = new ActivityNormalizedDto.LapData(
                5, Instant.now(), new BigDecimal("1000"), 370, 370, 82,
                139, 143, 77, 79, new BigDecimal("2.7"), new BigDecimal("2.8"),
                322, 351, 323, 92.9, 299.4, 8.97, 1035,
                0, 4, "active", 1
        );

        // When
        LapMetricsDto metrics = calculator.calculate(lapData);

        // Then
        assertEquals("Interval 5", metrics.lapName());
    }
}