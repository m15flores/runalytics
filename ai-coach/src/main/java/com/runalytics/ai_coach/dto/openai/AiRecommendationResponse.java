package com.runalytics.ai_coach.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response from OpenAI containing verdict and recommendations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponse {

    private TrainingVerdict verdict;

    @JsonProperty("verdict_rationale")
    private String verdictRationale;

    private List<AiRecommendation> recommendations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiRecommendation {
        private String type;
        private String priority;
        private String category;
        private String content;
        private String rationale;
    }
}
