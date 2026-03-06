package com.runalytics.ai_coach.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import lombok.Builder;

import static com.runalytics.ai_coach.dto.TrainingCycleContext.TrainingPhase;

import java.time.Instant;
import java.util.UUID;

@Builder
public record RecommendationDto(
        @JsonProperty("id") UUID id,
        @JsonProperty("userId") String userId,
        @JsonProperty("reportId") UUID reportId,
        @JsonProperty("type") RecommendationType type,
        @JsonProperty("priority") Priority priority,
        @JsonProperty("content") String content,
        @JsonProperty("rationale") String rationale,
        @JsonProperty("verdict") TrainingVerdict verdict,
        @JsonProperty("weekInCycle") Integer weekInCycle,
        @JsonProperty("trainingPhase") TrainingPhase trainingPhase,
        @JsonProperty("applied") Boolean applied,
        @JsonProperty("createdAt") Instant createdAt,
        @JsonProperty("expiresAt") Instant expiresAt
) {}