package com.runalytics.metrics_engine.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActivityNormalizedDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldSerializeAndDeserializeActivityNormalizedDto() throws Exception {
        // Given
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        var session = new ActivityNormalizedDto.SessionData(
                new BigDecimal("13138.37"),  // totalDistance
                4777,                         // totalTimerTime
                4987,                         // totalElapsedTime
                1048,                         // totalCalories
                140,                          // avgHeartRate
                150,                          // maxHeartRate
                78,                           // avgCadence
                84,                           // maxCadence
                new BigDecimal("2.75"),       // enhancedAvgSpeed
                new BigDecimal("3.984"),      // enhancedMaxSpeed
                349,                          // avgPower
                569,                          // maxPower
                351,                          // normalizedPower
                92.7,                         // avgVerticalOscillation
                291.8,                        // avgStanceTime
                8.82,                         // avgVerticalRatio
                1052,                         // avgStepLength
                36,                           // totalAscent
                34,                           // totalDescent
                3.5,                          // totalTrainingEffect
                0.0,                          // totalAnaerobicTrainingEffect
                112.56,                       // trainingLoadPeak
                75,                           // workoutFeel
                60,                           // workoutRpe
                Map.of("Z1", 0, "Z2", 2580, "Z3", 2010, "Z4", 0, "Z5", 0),  // timeInHrZones
                null,                         // timeInPowerZones
                195,                          // maxHeartRateConfig
                59,                           // restingHeartRate
                171,                          // thresholdHeartRate
                418                           // functionalThresholdPower
        );

        var dto = new ActivityNormalizedDto(
                activityId,
                "test-user",
                "Garmin Fenix 7",
                now,
                session,
                List.of(),  // laps vacío por ahora
                List.of(),  // samples vacío por ahora
                now
        );

        // When - Serialize to JSON
        String json = objectMapper.writeValueAsString(dto);

        // Then - JSON contiene los campos esperados
        assertNotNull(json);
        assertTrue(json.contains("test-user"));
        assertTrue(json.contains("13138.37"));

        // When - Deserialize from JSON
        ActivityNormalizedDto deserialized = objectMapper.readValue(json, ActivityNormalizedDto.class);

        // Then - DTO deserializado es igual al original
        assertEquals(dto.activityId(), deserialized.activityId());
        assertEquals(dto.userId(), deserialized.userId());
        assertEquals(dto.session().totalDistance(), deserialized.session().totalDistance());
        assertEquals(dto.session().avgHeartRate(), deserialized.session().avgHeartRate());
        assertEquals(5, deserialized.session().timeInHrZones().size());
    }
}