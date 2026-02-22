package com.runalytics.metrics_engine.mapper;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for ActivityMetrics entity ↔ ActivityMetricsDto
 *
 * Automatically generates implementation code at compile-time.
 * Spring will inject this as a @Component.
 */
@Mapper(componentModel = "spring")
public interface ActivityMetricsMapper {

    /**
     * Converts ActivityMetricsDto to ActivityMetrics entity.
     *
     * Note: id, createdAt and updatedAt are set by the service layer via injected Clock.
     *
     * @param dto the DTO from Kafka or service layer
     * @return the entity ready to be persisted
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ActivityMetrics toEntity(ActivityMetricsDto dto);

    /**
     * Converts ActivityMetrics entity to ActivityMetricsDto.
     *
     * Note: DTO has 'laps' field which doesn't exist in the entity.
     * This mapper only handles the activity metrics, not laps.
     *
     * @param entity the persisted entity
     * @return the DTO for API/Kafka responses
     */
    @Mapping(target = "laps", ignore = true)
    ActivityMetricsDto toDto(ActivityMetrics entity);

    /**
     * Converts list of entities to list of DTOs.
     *
     * @param entities list of persisted entities
     * @return list of DTOs
     */
    List<ActivityMetricsDto> toDtoList(List<ActivityMetrics> entities);
}