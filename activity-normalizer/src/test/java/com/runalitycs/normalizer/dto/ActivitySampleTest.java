package com.runalitycs.normalizer.dto;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ActivitySampleTest {

    @Test
    void shouldCreateActivitySampleWithAllFields() {
        // Given
        Instant timestamp = Instant.parse("2025-01-01T10:30:05Z");
        Double latitude = 40.416775;
        Double longitude = -3.703790;
        Integer heartRate = 145;
        Integer paceSecondsPerKm = 300;
        Double altitude = 650.5;
        Integer cadence = 85;

        // When
        ActivitySample sample = new ActivitySample(
                timestamp, latitude, longitude, heartRate,
                paceSecondsPerKm, altitude, cadence
        );

        // Then
        assertEquals(timestamp, sample.timestamp());
        assertEquals(latitude, sample.latitude());
        assertEquals(longitude, sample.longitude());
        assertEquals(heartRate, sample.heartRate());
        assertEquals(paceSecondsPerKm, sample.paceSecondsPerKm());
        assertEquals(altitude, sample.altitude());
        assertEquals(cadence, sample.cadence());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        Instant timestamp = Instant.now();

        // When - Solo campos obligatorios
        ActivitySample sample = new ActivitySample(
                timestamp, null, null, null, null, null, null
        );

        // Then
        assertNotNull(sample);
        assertEquals(timestamp, sample.timestamp());
        assertNull(sample.latitude());
        assertNull(sample.heartRate());
    }

    @Test
    void shouldBeImmutable() {
        // Given
        ActivitySample sample1 = new ActivitySample(
                Instant.now(), 40.0, -3.0, 145, 300, 650.0, 85
        );
        ActivitySample sample2 = new ActivitySample(
                sample1.timestamp(), sample1.latitude(), sample1.longitude(),
                sample1.heartRate(), sample1.paceSecondsPerKm(),
                sample1.altitude(), sample1.cadence()
        );

        // Then - Records tienen equals/hashCode automáticos
        assertEquals(sample1, sample2);
    }
}