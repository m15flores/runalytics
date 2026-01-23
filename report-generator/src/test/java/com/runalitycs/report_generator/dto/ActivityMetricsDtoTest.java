package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class ActivityMetricsDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void shouldCreateValidDto() {
        // Given & When
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        ActivityMetricsDto dto = ActivityMetricsDto.builder()
                .activityId(activityId)
                .userId("test-user")
                .startedAt(now)
                .totalDistance(new BigDecimal("10.5"))
                .totalDuration(3600)
                .totalCalories(600)
                .averagePace(343)
                .averageHeartRate(145)
                .averageCadence(170)
                .totalAscent(100)
                .totalDescent(100)
                .trainingEffect(3.5)
                .anaerobicTrainingEffect(1.2)
                .build();

        // Then
        assertNotNull(dto);
        assertEquals(activityId, dto.activityId());
        assertEquals("test-user", dto.userId());
        assertEquals(now, dto.startedAt());
        assertEquals(new BigDecimal("10.5"), dto.totalDistance());
        assertEquals(3600, dto.totalDuration());
        assertEquals(600, dto.totalCalories());
        assertEquals(343, dto.averagePace());
        assertEquals(145, dto.averageHeartRate());
        assertEquals(170, dto.averageCadence());
        assertEquals(100, dto.totalAscent());
        assertEquals(100, dto.totalDescent());
        assertEquals(3.5, dto.trainingEffect());
        assertEquals(1.2, dto.anaerobicTrainingEffect());
    }

    @Test
    void shouldCreateDtoWithMinimalFields() {
        // Given & When
        ActivityMetricsDto dto = ActivityMetricsDto.builder()
                .activityId(UUID.randomUUID())
                .userId("minimal-user")
                .startedAt(Instant.now())
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .build();

        // Then
        assertNotNull(dto);
        assertNull(dto.averagePace());
        assertNull(dto.averageHeartRate());
        assertNull(dto.averageCadence());
        assertNull(dto.hrZones());
    }

    @Test
    void shouldIncludeHrZones() {
        // Given
        Map<String, Integer> hrZones = new HashMap<>();
        hrZones.put("Z1", 600);
        hrZones.put("Z2", 2400);
        hrZones.put("Z3", 600);

        // When
        ActivityMetricsDto dto = ActivityMetricsDto.builder()
                .activityId(UUID.randomUUID())
                .userId("test-user")
                .startedAt(Instant.now())
                .totalDistance(new BigDecimal("10.0"))
                .totalDuration(3600)
                .hrZones(hrZones)
                .build();

        // Then
        assertNotNull(dto.hrZones());
        assertThat(dto.hrZones()).hasSize(3);
        assertThat(dto.hrZones().get("Z2")).isEqualTo(2400);
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        // Given
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.parse("2024-12-08T10:00:00Z");

        ActivityMetricsDto dto = ActivityMetricsDto.builder()
                .activityId(activityId)
                .userId("test-user")
                .startedAt(now)
                .totalDistance(new BigDecimal("10.5"))
                .totalDuration(3600)
                .averagePace(343)
                .build();

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"userId\":\"test-user\"");
        assertThat(json).contains("\"totalDistance\":10.5");
        assertThat(json).contains("\"totalDuration\":3600");
        assertThat(json).contains("\"averagePace\":343");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
                {
                    "activityId": "550e8400-e29b-41d4-a716-446655440000",
                    "userId": "test-user",
                    "startedAt": "2024-12-08T10:00:00Z",
                    "totalDistance": 10.5,
                    "totalDuration": 3600,
                    "averagePace": 343
                }
                """;

        // When
        ActivityMetricsDto dto = objectMapper.readValue(json, ActivityMetricsDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("test-user", dto.userId());
        assertEquals(new BigDecimal("10.5"), dto.totalDistance());
        assertEquals(3600, dto.totalDuration());
        assertEquals(343, dto.averagePace());
    }

    @Test
    void shouldHandleNullOptionalFieldsInJson() throws Exception {
        // Given
        String json = """
                {
                    "activityId": "550e8400-e29b-41d4-a716-446655440000",
                    "userId": "minimal-user",
                    "startedAt": "2024-12-08T10:00:00Z",
                    "totalDistance": 5.0,
                    "totalDuration": 1800
                }
                """;

        // When
        ActivityMetricsDto dto = objectMapper.readValue(json, ActivityMetricsDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("minimal-user", dto.userId());
        assertNull(dto.averagePace());
        assertNull(dto.averageHeartRate());
        assertNull(dto.hrZones());
    }
}
