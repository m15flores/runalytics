package com.runalytics.normalizer.dto;

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
        Double speed = 3.33;
        Integer power = null;
        Double distance = 500.0;

        // When
        ActivitySample sample = new ActivitySample(
                timestamp, latitude, longitude, heartRate,
                paceSecondsPerKm, altitude, cadence,
                speed, power, distance
        );

        // Then
        assertEquals(timestamp, sample.timestamp());
        assertEquals(latitude, sample.latitude());
        assertEquals(longitude, sample.longitude());
        assertEquals(heartRate, sample.heartRate());
        assertEquals(paceSecondsPerKm, sample.paceSecondsPerKm());
        assertEquals(altitude, sample.altitude());
        assertEquals(cadence, sample.cadence());
        assertEquals(speed, sample.speed());
        assertNull(sample.power());
        assertEquals(distance, sample.distance());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        Instant timestamp = Instant.now();

        // When - only timestamp required
        ActivitySample sample = new ActivitySample(
                timestamp, null, null, null, null, null, null, null, null, null
        );

        // Then
        assertNotNull(sample);
        assertEquals(timestamp, sample.timestamp());
        assertNull(sample.latitude());
        assertNull(sample.heartRate());
        assertNull(sample.speed());
        assertNull(sample.power());
    }

    @Test
    void shouldBeImmutable() {
        // Given
        ActivitySample sample1 = new ActivitySample(
                Instant.now(), 40.0, -3.0, 145, 300, 650.0, 85, 3.33, null, 500.0
        );
        ActivitySample sample2 = new ActivitySample(
                sample1.timestamp(), sample1.latitude(), sample1.longitude(),
                sample1.heartRate(), sample1.paceSecondsPerKm(),
                sample1.altitude(), sample1.cadence(),
                sample1.speed(), sample1.power(), sample1.distance()
        );

        // Then - Records have automatic equals/hashCode
        assertEquals(sample1, sample2);
    }
}