package com.runalytics.ai_coach.mapper;

import com.runalytics.ai_coach.dto.RecommendationDto;
import com.runalytics.ai_coach.entity.Recommendation;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {

    RecommendationDto toDto(Recommendation recommendation);
}