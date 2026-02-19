package com.runalytics.normalizer.entity;

import com.runalytics.normalizer.dto.ActivitySample;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActivityTest {

    @Test
    void shouldCreateActivityWithRequiredFields() {
        // Given
        Activity activity = new Activity();
        activity.setUserId("user-12345");
        activity.setStartedAt(Instant.parse("2025-01-01T10:30:00Z"));

        // When & Then
        assertNotNull(activity);
        assertEquals("user-12345", activity.getUserId());
        assertEquals(Instant.parse("2025-01-01T10:30:00Z"), activity.getStartedAt());
        assertNull(activity.getId()); // No persisted yet
    }

    @Test
    void shouldSetOptionalFields() {
        // Given
        Activity activity = new Activity();
        activity.setUserId("user-12345");
        activity.setStartedAt(Instant.now());
        activity.setDevice("Garmin-Fenix-7-Pro");
        activity.setDurationSeconds(2780);
        activity.setDistanceMeters(new BigDecimal("10042.50"));

        List<ActivitySample> samples = List.of(
                new ActivitySample(
                        Instant.parse("2025-01-01T10:30:05Z"),
                        40.416775, -3.703790, 145, 300, 650.5, 85
                )
        );
        activity.setSamples(samples);

        // Then
        assertEquals("Garmin-Fenix-7-Pro", activity.getDevice());
        assertEquals(2780, activity.getDurationSeconds());
        assertEquals(new BigDecimal("10042.50"), activity.getDistanceMeters());
        assertEquals(1, activity.getSamples().size());
    }

    @Test
    void shouldAcceptCreatedAtAndUpdatedAt() {
        // Given
        Instant now = Instant.parse("2025-01-01T10:00:00Z");
        Activity activity = new Activity();
        activity.setUserId("user-12345");
        activity.setStartedAt(Instant.parse("2025-01-01T10:30:00Z"));

        // When
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);

        // Then
        assertEquals(now, activity.getCreatedAt());
        assertEquals(now, activity.getUpdatedAt());
    }
}