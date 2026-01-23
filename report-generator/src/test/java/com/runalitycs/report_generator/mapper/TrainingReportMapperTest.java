package com.runalitycs.report_generator.mapper;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.TrainingReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class TrainingReportMapperTest {

    private TrainingReportMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(TrainingReportMapper.class);
    }

    @Test
    void shouldMapEntityToDto() {
        // Given
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        TrainingReport entity = TrainingReport.builder()
                .id(id)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Report")
                .summaryJson("{\"totalKm\": 52.0}")
                .triggerActivityId(activityId)
                .build();

        entity.setCreatedAt(now);

        // When
        TrainingReportDto dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("test-user", dto.userId());
        assertEquals(49, dto.weekNumber());
        assertEquals(2024, dto.year());
        assertEquals("# Report", dto.markdownContent());
        assertEquals("{\"totalKm\": 52.0}", dto.summaryJson());
        assertEquals(activityId, dto.triggerActivityId());
        assertEquals(now, dto.createdAt());
    }

    @Test
    void shouldMapDtoToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.now();

        TrainingReportDto dto = new TrainingReportDto(
                id,
                "test-user",
                49,
                2024,
                "# Report",
                "{\"totalKm\": 52.0}",
                now,
                activityId
        );

        // When
        TrainingReport entity = mapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("test-user", entity.getUserId());
        assertEquals(49, entity.getWeekNumber());
        assertEquals(2024, entity.getYear());
        assertEquals("# Report", entity.getMarkdownContent());
        assertEquals("{\"totalKm\": 52.0}", entity.getSummaryJson());
        assertEquals(activityId, entity.getTriggerActivityId());
    }

    @Test
    void shouldMapEntityToDtoWithNullOptionalFields() {
        // Given
        TrainingReport entity = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("minimal-user")
                .weekNumber(1)
                .year(2024)
                .markdownContent("# Minimal")
                .build();

        // When
        TrainingReportDto dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.id());
        assertEquals("minimal-user", dto.userId());
        assertEquals(1, dto.weekNumber());
        assertEquals(2024, dto.year());
        assertEquals("# Minimal", dto.markdownContent());
        assertNull(dto.summaryJson());
        assertNull(dto.triggerActivityId());
    }

    @Test
    void shouldMapDtoToEntityWithNullOptionalFields() {
        // Given
        TrainingReportDto dto = new TrainingReportDto(
                UUID.randomUUID(),
                "minimal-user",
                1,
                2024,
                "# Minimal",
                null,
                null,
                null
        );

        // When
        TrainingReport entity = mapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getId());
        assertEquals("minimal-user", entity.getUserId());
        assertEquals(1, entity.getWeekNumber());
        assertEquals(2024, entity.getYear());
        assertEquals("# Minimal", entity.getMarkdownContent());
        assertNull(entity.getSummaryJson());
        assertNull(entity.getTriggerActivityId());
    }

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        // When
        TrainingReportDto dto = mapper.toDto(null);

        // Then
        assertNull(dto);
    }

    @Test
    void shouldReturnNullWhenDtoIsNull() {
        // When
        TrainingReport entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        // Given - Entity with all fields populated
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-12-01T10:00:00Z");

        TrainingReport entity = TrainingReport.builder()
                .id(id)
                .userId("mario-runner")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Training Report - Week 49/2024")
                .summaryJson("{\"totalKm\": 52.0, \"trend\": \"improving\"}")
                .triggerActivityId(activityId)
                .build();

        entity.setCreatedAt(createdAt);

        // When
        TrainingReportDto dto = mapper.toDto(entity);

        // Then - Verify all fields are mapped
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.userId()).isEqualTo("mario-runner");
        assertThat(dto.weekNumber()).isEqualTo(49);
        assertThat(dto.year()).isEqualTo(2024);
        assertThat(dto.markdownContent()).isEqualTo("# Training Report - Week 49/2024");
        assertThat(dto.summaryJson()).isEqualTo("{\"totalKm\": 52.0, \"trend\": \"improving\"}");
        assertThat(dto.triggerActivityId()).isEqualTo(activityId);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
    }

    @Test
    void shouldMapBidirectionally() {
        // Given - Start with DTO
        UUID id = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();

        TrainingReportDto originalDto = new TrainingReportDto(
                id,
                "bidirectional-user",
                50,
                2024,
                "# Bidirectional Test",
                "{\"test\": true}",
                null,
                activityId
        );

        // When - Map DTO → Entity → DTO
        TrainingReport entity = mapper.toEntity(originalDto);
        TrainingReportDto resultDto = mapper.toDto(entity);

        // Then - Core fields should match (ignoring timestamps)
        assertThat(resultDto.id()).isEqualTo(originalDto.id());
        assertThat(resultDto.userId()).isEqualTo(originalDto.userId());
        assertThat(resultDto.weekNumber()).isEqualTo(originalDto.weekNumber());
        assertThat(resultDto.year()).isEqualTo(originalDto.year());
        assertThat(resultDto.markdownContent()).isEqualTo(originalDto.markdownContent());
        assertThat(resultDto.summaryJson()).isEqualTo(originalDto.summaryJson());
        assertThat(resultDto.triggerActivityId()).isEqualTo(originalDto.triggerActivityId());
    }

    @Test
    void shouldIgnoreTimestampWhenMappingDtoToEntity() {
        // Given - DTO with timestamp (should be ignored when creating entity)
        Instant now = Instant.now();
        TrainingReportDto dto = new TrainingReportDto(
                UUID.randomUUID(),
                "timestamp-user",
                1,
                2024,
                "# Timestamp Test",
                "{}",
                now,
                UUID.randomUUID()
        );

        // When
        TrainingReport entity = mapper.toEntity(dto);

        // Then - Timestamp should NOT be set (it's managed by @PrePersist)
        assertNull(entity.getCreatedAt());
    }

    @Test
    void shouldPreserveIdWhenMapping() {
        // Given
        UUID specificId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        TrainingReportDto dto = new TrainingReportDto(
                specificId,
                "preserve-id-user",
                25,
                2024,
                "# Preserve ID Test",
                "{}",
                null,
                UUID.randomUUID()
        );

        // When
        TrainingReport entity = mapper.toEntity(dto);

        // Then
        assertThat(entity.getId()).isEqualTo(specificId);
    }

    @Test
    void shouldHandleLongMarkdownContent() {
        // Given
        String longMarkdown = "# Report\n\n".repeat(100); // 1000 chars

        TrainingReport entity = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent(longMarkdown)
                .build();

        // When
        TrainingReportDto dto = mapper.toDto(entity);

        // Then
        assertThat(dto.markdownContent().length()).isGreaterThanOrEqualTo(1000);
        assertThat(dto.markdownContent()).startsWith("# Report");
    }

    @Test
    void shouldMapComplexSummaryJson() {
        // Given
        String complexJson = "{\"weekNumber\":49,\"year\":2024,\"totalActivities\":4,\"totalKm\":52.0,\"trend\":\"improving\",\"zones\":{\"Z1\":600,\"Z2\":2400}}";

        TrainingReport entity = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Report")
                .summaryJson(complexJson)
                .build();

        // When
        TrainingReportDto dto = mapper.toDto(entity);

        // Then
        assertThat(dto.summaryJson()).isEqualTo(complexJson);
        assertThat(dto.summaryJson()).contains("\"trend\":\"improving\"");
        assertThat(dto.summaryJson()).contains("\"zones\"");
    }
}