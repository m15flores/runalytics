package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class TrainingReportDtoTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void shouldCreateValidDto() {
        // Given & When
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        TrainingReportDto dto = new TrainingReportDto(
                id,
                "test-user",
                49,
                2024,
                "# Training Report",
                "{\"totalKm\": 52.0}",
                now,
                activityId
        );

        // Then
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("test-user", dto.userId());
        assertEquals(49, dto.weekNumber());
        assertEquals(2024, dto.year());
        assertEquals("# Training Report", dto.markdownContent());
        assertEquals("{\"totalKm\": 52.0}", dto.summaryJson());
        assertEquals(now, dto.createdAt());
        assertEquals(activityId, dto.triggerActivityId());
    }

    @Test
    void shouldCreateDtoWithMinimalFields() {
        // Given & When
        TrainingReportDto dto = new TrainingReportDto(
                UUID.randomUUID(),
                "minimal-user",
                1,
                2024,
                "# Minimal Report",
                null,
                null,
                null
        );

        // Then
        assertNotNull(dto);
        assertEquals("minimal-user", dto.userId());
        assertNull(dto.summaryJson());
        assertNull(dto.triggerActivityId());
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2024-12-08T10:00:00Z");

        TrainingReportDto dto = new TrainingReportDto(
                id,
                "test-user",
                49,
                2024,
                "# Report",
                "{\"totalKm\": 52.0}",
                now,
                UUID.randomUUID()
        );

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"userId\":\"test-user\"");
        assertThat(json).contains("\"weekNumber\":49");
        assertThat(json).contains("\"year\":2024");
        assertThat(json).contains("\"markdownContent\":\"# Report\"");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
                {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "userId": "test-user",
                    "weekNumber": 49,
                    "year": 2024,
                    "markdownContent": "# Report",
                    "summaryJson": "{\\"totalKm\\": 52.0}",
                    "createdAt": "2024-12-08T10:00:00Z",
                    "triggerActivityId": "550e8400-e29b-41d4-a716-446655440001"
                }
                """;

        // When
        TrainingReportDto dto = objectMapper.readValue(json, TrainingReportDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("test-user", dto.userId());
        assertEquals(49, dto.weekNumber());
        assertEquals(2024, dto.year());
        assertEquals("# Report", dto.markdownContent());
    }

    @Test
    void shouldHandleNullOptionalFieldsInJson() throws Exception {
        // Given
        String json = """
                {
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "userId": "minimal-user",
                    "weekNumber": 1,
                    "year": 2024,
                    "markdownContent": "# Minimal"
                }
                """;

        // When
        TrainingReportDto dto = objectMapper.readValue(json, TrainingReportDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("minimal-user", dto.userId());
        assertNull(dto.summaryJson());
        assertNull(dto.createdAt());
        assertNull(dto.triggerActivityId());
    }

    @Test
    void shouldHandleLongMarkdownContent() {
        // Given
        String longMarkdown = "# Report\n\n".repeat(100); // Long content

        // When
        TrainingReportDto dto = new TrainingReportDto(
                UUID.randomUUID(),
                "test-user",
                49,
                2024,
                longMarkdown,
                "{}",
                Instant.now(),
                UUID.randomUUID()
        );

        // Then
        assertThat(dto.markdownContent().length()).isGreaterThanOrEqualTo(1000);
    }
}
