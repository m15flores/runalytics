package com.runalytics.normalizer.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        Instant normalizedAt = Instant.now();

        ActivityNormalizedDto.SessionData session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("10042.50"), 2780, 2800, 320,
                148, 172, 85, 92,
                new BigDecimal("3.58"), new BigDecimal("4.12"),
                null, null, null,
                null, null, null, null,
                45, 3,
                null, null, null,
                null, null,
                Map.of("Z1", 120, "Z2", 1800, "Z3", 760, "Z4", 100, "Z5", 0), null,
                null, null, null, null
        );

        List<ActivityNormalizedDto.SampleData> samples = List.of(
                new ActivityNormalizedDto.SampleData(
                        Instant.parse("2025-01-01T10:30:05Z"),
                        40.416775, -3.703790, 145, 85, 650.5, 3.33, null, null
                )
        );

        // When
        ActivityNormalizedDto dto = new ActivityNormalizedDto(
                activityId, userId, device, startedAt,
                session, List.of(), samples, normalizedAt
        );

        // Then
        assertEquals(activityId, dto.activityId());
        assertEquals(userId, dto.userId());
        assertEquals(device, dto.device());
        assertEquals(startedAt, dto.startedAt());
        assertNotNull(dto.session());
        assertEquals(new BigDecimal("10042.50"), dto.session().totalDistance());
        assertEquals(148, dto.session().avgHeartRate());
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
                null, List.of(), List.of(), normalizedAt
        );

        // Then
        assertNotNull(dto);
        assertNull(dto.device());
        assertNull(dto.session());
        assertTrue(dto.laps().isEmpty());
        assertTrue(dto.samples().isEmpty());
    }
}