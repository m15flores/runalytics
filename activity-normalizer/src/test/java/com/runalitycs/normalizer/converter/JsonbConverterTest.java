package com.runalitycs.normalizer.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.runalitycs.normalizer.dto.ActivitySample;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonbConverterTest {

    private JsonbConverter converter;

    @BeforeEach
    void setUp() {
        converter = new JsonbConverter();
    }

    @Test
    void shouldConvertListToJson() throws JsonProcessingException {
        // Given
        Instant timestamp = Instant.parse("2025-01-01T10:30:05Z");
        List<ActivitySample> samples = List.of(
                new ActivitySample(timestamp, 40.416775, -3.703790, 145, 300, 650.5, 85)
        );

        // When
        String json = converter.convertToDatabaseColumn(samples);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("40.416775"));
        assertTrue(json.contains("145"));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String expectedTimestamp = mapper.writeValueAsString(timestamp);
        assertTrue(json.contains(expectedTimestamp));
    }

    @Test
    void shouldConvertJsonToList() {
        // Given
        String json = """
            [
              {
                "timestamp": "2025-01-01T10:30:05Z",
                "latitude": 40.416775,
                "longitude": -3.703790,
                "heartRate": 145,
                "paceSecondsPerKm": 300,
                "altitude": 650.5,
                "cadence": 85
              }
            ]
            """;

        // When
        List<ActivitySample> samples = converter.convertToEntityAttribute(json);

        // Then
        assertNotNull(samples);
        assertEquals(1, samples.size());
        assertEquals(145, samples.get(0).heartRate());
        assertEquals(40.416775, samples.get(0).latitude());
    }

    @Test
    void shouldRoundTripCorrectly() {
        // Given
        List<ActivitySample> originalSamples = List.of(
                new ActivitySample(
                        Instant.parse("2025-01-01T10:30:05Z"),
                        40.416775, -3.703790, 145, 300, 650.5, 85
                ),
                new ActivitySample(
                        Instant.parse("2025-01-01T10:30:06Z"),
                        40.416780, -3.703795, 147, 298, 651.0, 86
                )
        );

        // When - Convertir a JSON y de vuelta
        String json = converter.convertToDatabaseColumn(originalSamples);
        List<ActivitySample> deserializedSamples = converter.convertToEntityAttribute(json);

        // Then - Deben ser iguales
        assertEquals(originalSamples.size(), deserializedSamples.size());
        assertEquals(originalSamples.get(0), deserializedSamples.get(0));
        assertEquals(originalSamples.get(1), deserializedSamples.get(1));
    }

    @Test
    void shouldHandleNullList() {
        // When
        String json = converter.convertToDatabaseColumn(null);

        // Then
        assertNull(json);
    }

    @Test
    void shouldHandleEmptyList() {
        // When
        String json = converter.convertToDatabaseColumn(List.of());

        // Then
        assertNull(json);
    }

    @Test
    void shouldHandleNullJson() {
        // When
        List<ActivitySample> samples = converter.convertToEntityAttribute(null);

        // Then
        assertNotNull(samples);
        assertTrue(samples.isEmpty());
    }

    @Test
    void shouldHandleEmptyJson() {
        // When
        List<ActivitySample> samples = converter.convertToEntityAttribute("");

        // Then
        assertNotNull(samples);
        assertTrue(samples.isEmpty());
    }
}