package com.runalytics.ai_coach.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.dto.RecommendationGeneratedEventDto;
import com.runalytics.ai_coach.dto.TrainingReportEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kafka producer for recommendation generated events
 */
@Component
@Slf4j
public class RecommendationGeneratedProducer {

    private final KafkaTemplate<String, RecommendationGeneratedEventDto> kafkaTemplate;
    private final String topic;
    private final ObjectMapper objectMapper;

    public RecommendationGeneratedProducer(
            KafkaTemplate<String, RecommendationGeneratedEventDto> kafkaTemplate,
            @Value("${app.kafka.topics.recommendations-generated}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Publish recommendations generated event to Kafka
     */
    public void publishRecommendations(
            TrainingReportEventDto reportEvent,
            List<Recommendation> recommendations) {

        log.info("Publishing recommendations generated event: reportId={}, userId={}, count={}",
                reportEvent.getReportId(), reportEvent.getUserId(), recommendations.size());

        // Extract verdict from first recommendation (all have same verdict)
        TrainingVerdict verdict = recommendations.isEmpty()
                ? TrainingVerdict.VALID
                : recommendations.get(0).getVerdict();

        // Extract verdict rationale from metadata
        String verdictRationale = recommendations.isEmpty()
                ? "No recommendations needed"
                : extractVerdictRationale(recommendations.get(0));

        // Build event
        RecommendationGeneratedEventDto event = RecommendationGeneratedEventDto.builder()
                .reportId(reportEvent.getReportId())
                .userId(reportEvent.getUserId())
                .weekNumber(reportEvent.getWeekNumber())
                .year(reportEvent.getYear())
                .verdict(verdict)
                .verdictRationale(verdictRationale)
                .recommendations(recommendations.stream()
                        .map(this::toRecommendationSummary)
                        .collect(Collectors.toList()))
                .generatedAt(Instant.now())
                .build();

        // Publish with userId as key (for partitioning)
        kafkaTemplate.send(topic, reportEvent.getUserId(), event);

        log.info("Successfully published recommendations event for report: {}", reportEvent.getReportId());
    }

    /**
     * Convert Recommendation to summary DTO
     */
    private RecommendationGeneratedEventDto.RecommendationSummary toRecommendationSummary(
            Recommendation recommendation) {

        return RecommendationGeneratedEventDto.RecommendationSummary.builder()
                .recommendationId(recommendation.getId())
                .type(recommendation.getType())
                .priority(recommendation.getPriority())
                .category(extractCategory(recommendation))
                .content(recommendation.getContent())
                .build();
    }

    /**
     * Extract category from metadata JSON
     */
    private String extractCategory(Recommendation recommendation) {
        if (recommendation.getMetadata() == null) {
            return "ADJUSTMENT";
        }

        try {
            JsonNode metadata = objectMapper.readTree(recommendation.getMetadata());
            return metadata.has("category")
                    ? metadata.get("category").asText()
                    : "ADJUSTMENT";
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata for category, using default: {}", e.getMessage());
            return "ADJUSTMENT";
        }
    }

    /**
     * Extract verdict rationale from metadata JSON
     */
    private String extractVerdictRationale(Recommendation recommendation) {
        if (recommendation.getMetadata() == null) {
            return "Analysis complete";
        }

        try {
            JsonNode metadata = objectMapper.readTree(recommendation.getMetadata());
            return metadata.has("verdict_rationale")
                    ? metadata.get("verdict_rationale").asText()
                    : "Analysis complete";
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse metadata for verdict rationale: {}", e.getMessage());
            return "Analysis complete";
        }
    }
}
