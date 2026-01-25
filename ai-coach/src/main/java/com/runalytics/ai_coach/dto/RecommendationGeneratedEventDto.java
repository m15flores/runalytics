package com.runalytics.ai_coach.dto;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event published to Kafka when recommendations are generated
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationGeneratedEventDto {

    private UUID reportId;
    private String userId;
    private Integer weekNumber;
    private Integer year;
    private TrainingVerdict verdict;
    private String verdictRationale;
    private List<RecommendationSummary> recommendations;
    private Instant generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationSummary {
        private UUID recommendationId;
        private RecommendationType type;
        private Priority priority;
        private String category;
        private String content;
    }
}
