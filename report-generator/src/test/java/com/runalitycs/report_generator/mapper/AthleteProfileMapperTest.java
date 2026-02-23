package com.runalitycs.report_generator.mapper;

import com.runalitycs.report_generator.dto.AthleteProfileDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AthleteProfileMapperTest {

    private AthleteProfileMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AthleteProfileMapper.class);
    }

    @Test
    void shouldMapEntityToDto() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        AthleteProfile entity = AthleteProfile.builder()
                .id(id)
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();

        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        // When
        AthleteProfileDto dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(id, dto.id());
        assertEquals("test-user", dto.userId());
        assertEquals("Test Runner", dto.name());
        assertEquals(30, dto.age());
        assertEquals(70.0, dto.weight());
        assertEquals(190, dto.maxHeartRate());
        assertEquals(now, dto.createdAt());
        assertEquals(now, dto.updatedAt());
    }

    @Test
    void shouldMapDtoToEntity() {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();

        AthleteProfileDto dto = new AthleteProfileDto(
                id,
                "test-user",
                "Test Runner",
                30,
                70.0,
                190,
                null,
                now,
                now
        );

        // When
        AthleteProfile entity = mapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals(id, entity.getId());
        assertEquals("test-user", entity.getUserId());
        assertEquals("Test Runner", entity.getName());
        assertEquals(30, entity.getAge());
        assertEquals(70.0, entity.getWeight());
        assertEquals(190, entity.getMaxHeartRate());
    }

    @Test
    void shouldMapEntityToDtoWithNullOptionalFields() {
        // Given
        AthleteProfile entity = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("minimal-user")
                .name("Minimal Profile")
                .build();

        // When
        AthleteProfileDto dto = mapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.id());
        assertEquals("minimal-user", dto.userId());
        assertEquals("Minimal Profile", dto.name());
        assertNull(dto.age());
        assertNull(dto.weight());
        assertNull(dto.maxHeartRate());
    }

    @Test
    void shouldMapDtoToEntityWithNullOptionalFields() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                UUID.randomUUID(),
                "minimal-user",
                "Minimal Profile",
                null,
                null,
                null,
                null,
                null,
                null
        );

        // When
        AthleteProfile entity = mapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertNotNull(entity.getId());
        assertEquals("minimal-user", entity.getUserId());
        assertEquals("Minimal Profile", entity.getName());
        assertNull(entity.getAge());
        assertNull(entity.getWeight());
        assertNull(entity.getMaxHeartRate());
    }

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        // When
        AthleteProfileDto dto = mapper.toDto(null);

        // Then
        assertNull(dto);
    }

    @Test
    void shouldReturnNullWhenDtoIsNull() {
        // When
        AthleteProfile entity = mapper.toEntity(null);

        // Then
        assertNull(entity);
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        // Given - Entity with all fields populated
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2024-12-01T10:00:00Z");
        Instant updatedAt = Instant.parse("2024-12-08T15:30:00Z");

        AthleteProfile entity = AthleteProfile.builder()
                .id(id)
                .userId("mario-runner")
                .name("Mario")
                .age(30)
                .weight(70.5)
                .maxHeartRate(192)
                .build();

        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(updatedAt);

        // When
        AthleteProfileDto dto = mapper.toDto(entity);

        // Then - Verify all fields are mapped
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.userId()).isEqualTo("mario-runner");
        assertThat(dto.name()).isEqualTo("Mario");
        assertThat(dto.age()).isEqualTo(30);
        assertThat(dto.weight()).isEqualTo(70.5);
        assertThat(dto.maxHeartRate()).isEqualTo(192);
        assertThat(dto.createdAt()).isEqualTo(createdAt);
        assertThat(dto.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void shouldMapBidirectionally() {
        // Given - Start with DTO
        UUID id = UUID.randomUUID();
        AthleteProfileDto originalDto = new AthleteProfileDto(
                id,
                "bidirectional-user",
                "Bidirectional Test",
                35,
                75.0,
                185,
                null,
                null,
                null
        );

        // When - Map DTO → Entity → DTO
        AthleteProfile entity = mapper.toEntity(originalDto);
        AthleteProfileDto resultDto = mapper.toDto(entity);

        // Then - Core fields should match (ignoring timestamps)
        assertThat(resultDto.id()).isEqualTo(originalDto.id());
        assertThat(resultDto.userId()).isEqualTo(originalDto.userId());
        assertThat(resultDto.name()).isEqualTo(originalDto.name());
        assertThat(resultDto.age()).isEqualTo(originalDto.age());
        assertThat(resultDto.weight()).isEqualTo(originalDto.weight());
        assertThat(resultDto.maxHeartRate()).isEqualTo(originalDto.maxHeartRate());
    }

    @Test
    void shouldIgnoreTimestampsWhenMappingDtoToEntity() {
        // Given - DTO with timestamps (should be ignored when creating entity)
        Instant now = Instant.now();
        AthleteProfileDto dto = new AthleteProfileDto(
                UUID.randomUUID(),
                "timestamp-user",
                "Timestamp Test",
                30,
                70.0,
                190,
                null,
                now,
                now
        );

        // When
        AthleteProfile entity = mapper.toEntity(dto);

        // Then - Timestamps should NOT be set (managed by service layer)
        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());
    }

    @Test
    void shouldPreserveIdWhenMapping() {
        // Given
        UUID specificId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        AthleteProfileDto dto = new AthleteProfileDto(
                specificId,
                "preserve-id-user",
                "Preserve ID Test",
                25,
                65.0,
                195,
                null,
                null,
                null
        );

        // When
        AthleteProfile entity = mapper.toEntity(dto);

        // Then
        assertThat(entity.getId()).isEqualTo(specificId);
    }
}