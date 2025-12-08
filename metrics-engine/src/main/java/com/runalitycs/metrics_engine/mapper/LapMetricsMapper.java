package com.runalitycs.metrics_engine.mapper;

import com.runalitycs.metrics_engine.dto.LapMetricsDto;
import com.runalitycs.metrics_engine.entity.LapMetrics;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for LapMetrics entity ↔ LapMetricsDto
 *
 * Automatically generates implementation code at compile-time.
 * Spring will inject this as a @Component.
 */
@Mapper(componentModel = "spring")
public interface LapMetricsMapper {

    /**
     * Converts LapMetricsDto to LapMetrics entity.
     *
     * Note: activityId must be set separately when saving laps.
     * Entity fields (id, createdAt, updatedAt) are auto-generated via @PrePersist.
     *
     * @param dto the DTO from service layer
     * @return the entity ready to be persisted
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activityId", ignore = true)  // Set separately in service
    @Mapping(target = "pace", ignore = true)  // Entity has 'pace' field, DTO doesn't
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    LapMetrics toEntity(LapMetricsDto dto);

    /**
     * Converts LapMetrics entity to LapMetricsDto.
     *
     * @param entity the persisted entity
     * @return the DTO for API/Kafka responses
     */
    LapMetricsDto toDto(LapMetrics entity);

    /**
     * Converts list of entities to list of DTOs.
     *
     * @param entities list of persisted entities
     * @return list of DTOs
     */
    List<LapMetricsDto> toDtoList(List<LapMetrics> entities);

    /**
     * Converts list of DTOs to list of entities.
     *
     * @param dtos list of DTOs
     * @return list of entities (activityId must be set separately)
     */
    List<LapMetrics> toEntityList(List<LapMetricsDto> dtos);
}