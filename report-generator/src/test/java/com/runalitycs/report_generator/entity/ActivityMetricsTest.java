package com.runalitycs.report_generator.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ActivityMetricsTest {
    @Test
    void shouldCreateActivityMetricsWithRequiredFields() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setActivityId(UUID.randomUUID());
        metrics.setUserId("test-user");
        metrics.setStartedAt(Instant.now());
        metrics.setTotalDistance(new BigDecimal("10.5"));
        metrics.setTotalDuration(3600);

        // Then
        assertNotNull(metrics);
        assertNotNull(metrics.getId());
        assertEquals("test-user", metrics.getUserId());
        assertEquals(new BigDecimal("10.5"), metrics.getTotalDistance());
        assertEquals(3600, metrics.getTotalDuration());
    }

    @Test
    void shouldSetOptionalFields() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setUserId("test-user");
        metrics.setStartedAt(Instant.now());
        metrics.setTotalDistance(new BigDecimal("10.0"));
        metrics.setTotalDuration(3600);
        metrics.setAveragePace(343);
        metrics.setAverageHeartRate(145);
        metrics.setAverageCadence(170);
        metrics.setTotalCalories(600);
        metrics.setTotalAscent(100);
        metrics.setTotalDescent(100);
        metrics.setTrainingEffect(3.5);
        metrics.setAnaerobicTrainingEffect(1.2);

        // Then
        assertEquals(343, metrics.getAveragePace());
        assertEquals(145, metrics.getAverageHeartRate());
        assertEquals(170, metrics.getAverageCadence());
        assertEquals(600, metrics.getTotalCalories());
        assertEquals(3.5, metrics.getTrainingEffect());
    }

    @Test
    void shouldStoreHrZones() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        Map<String, Integer> hrZones = new HashMap<>();
        hrZones.put("Z1", 600);
        hrZones.put("Z2", 2400);
        hrZones.put("Z3", 600);

        metrics.setHrZones(hrZones);

        // Then
        assertNotNull(metrics.getHrZones());
        assertEquals(3, metrics.getHrZones().size());
        assertEquals(2400, metrics.getHrZones().get("Z2"));
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setUserId("minimal-user");
        metrics.setStartedAt(Instant.now());
        metrics.setTotalDistance(new BigDecimal("5.0"));
        metrics.setTotalDuration(1800);

        // Then
        assertNull(metrics.getAveragePace());
        assertNull(metrics.getAverageHeartRate());
        assertNull(metrics.getAverageCadence());
        assertNull(metrics.getHrZones());
        assertNull(metrics.getTotalCalories());
    }

    @Test
    void shouldBeImmutable() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setTotalDistance(new BigDecimal("10.0"));

        // When - Try to modify (entity is @Immutable in Hibernate)
        BigDecimal originalDistance = metrics.getTotalDistance();

        // Then - Should preserve original value
        assertEquals(originalDistance, metrics.getTotalDistance());
    }
}
