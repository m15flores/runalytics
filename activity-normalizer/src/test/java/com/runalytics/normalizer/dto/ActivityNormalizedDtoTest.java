package com.runalytics.normalizer.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityNormalizedDtoTest {

    @Test
    void shouldCreateActivityNormalizedDtoWithAllFields() {
        // Given
        UUID activityId = UUID.randomUUID();
        String userId = "user-12345";
        String device = "Garmin-Fenix-7-Pro";
        Instant startedAt = Instant.parse("2025-01-01T10:30:00Z");
        Integer durationSeconds = 2780;
        BigDecimal distanceMeters = new BigDecimal("10042.50");
        List<ActivitySample> samples = List.of(
                new ActivitySample(
                        Instant.parse("2025-01-01T10:30:05Z"),
                        40.416775, -3.703790, 145, 300, 650.5, 85
                )
        );
        Instant normalizedAt = Instant.now();

        // When
        ActivityNormalizedDto dto = new ActivityNormalizedDto(
                activityId, userId, device, startedAt,
                durationSeconds, distanceMeters, samples, normalizedAt
        );

        // Then
        assertEquals(activityId, dto.activityId());
        assertEquals(userId, dto.userId());
        assertEquals(device, dto.device());
        assertEquals(startedAt, dto.startedAt());
        assertEquals(durationSeconds, dto.durationSeconds());
        assertEquals(distanceMeters, dto.distanceMeters());
        assertEquals(1, dto.samples().size());
        assertEquals(normalizedAt, dto.normalizedAt());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        UUID activityId = UUID.randomUUID();
        String userId = "user-12345";
        Instant startedAt = Instant.now();
        Instant normalizedAt = Instant.now();

        // When
        ActivityNormalizedDto dto = new ActivityNormalizedDto(
                activityId, userId, null, startedAt,
                null, null, List.of(), normalizedAt
        );

        // Then
        assertNotNull(dto);
        assertNull(dto.device());
        assertNull(dto.durationSeconds());
        assertNull(dto.distanceMeters());
        assertTrue(dto.samples().isEmpty());
    }
}