package com.runalytics.ai_coach.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class RecommendationTest {

    @Test
    void shouldCreateRecommendationWithRequiredFields() {
        // Given
        UUID id = UUID.randomUUID();
        String userId = "test-user";
        UUID reportId = UUID.randomUUID();
        RecommendationType type = RecommendationType.TRAINING_VOLUME;
        Priority priority = Priority.HIGH;
        String content = "Reduce your weekly mileage by 20% to prevent overtraining";
        String rationale = "Your 4-week training volume increased by 45%, which exceeds the safe 10% weekly increase guideline";
        Instant createdAt = Instant.now();

        // When
        Recommendation recommendation = Recommendation.builder()
                .id(id)
                .userId(userId)
                .reportId(reportId)
                .type(type)
                .priority(priority)
                .content(content)
                .rationale(rationale)
                .createdAt(createdAt)
                .build();

        // Then
        assertThat(recommendation.getId()).isEqualTo(id);
        assertThat(recommendation.getUserId()).isEqualTo(userId);
        assertThat(recommendation.getReportId()).isEqualTo(reportId);
        assertThat(recommendation.getType()).isEqualTo(type);
        assertThat(recommendation.getPriority()).isEqualTo(priority);
        assertThat(recommendation.getContent()).isEqualTo(content);
        assertThat(recommendation.getRationale()).isEqualTo(rationale);
        assertThat(recommendation.getCreatedAt()).isEqualTo(createdAt);
    }
    @Test
    void shouldCreateRecommendationWithOptionalFields() {
        // Given
        String metadata = "{\"confidence\": 0.95, \"source\": \"trend_analysis\"}";
        Instant expiresAt = Instant.now().plusSeconds(7 * 24 * 3600); // 7 days
        Boolean applied = true;

        // When
        Recommendation recommendation = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Take an extra rest day this week")
                .rationale("Your average heart rate is elevated")
                .metadata(metadata)
                .expiresAt(expiresAt)
                .applied(applied)
                .build();

        // Then
        assertThat(recommendation.getMetadata()).isEqualTo(metadata);
        assertThat(recommendation.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(recommendation.getApplied()).isTrue();
    }

    @Test
    void shouldSetCreatedAtViaBuilder() {
        // Given
        Instant createdAt = Instant.now();

        // When
        Recommendation recommendation = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.PACE)
                .priority(Priority.LOW)
                .content("Try to maintain a more consistent pace")
                .rationale("Your pace variance is high")
                .createdAt(createdAt)
                .build();

        // Then
        assertThat(recommendation.getCreatedAt()).isEqualTo(createdAt);
        assertThat(recommendation.getCreatedAt()).isBefore(Instant.now().plusSeconds(1));
    }

    @Test
    void shouldDefaultAppliedToFalse() {
        // When
        Recommendation recommendation = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.CADENCE)
                .priority(Priority.MEDIUM)
                .content("Increase your cadence to 180 spm")
                .rationale("Your current cadence is below optimal")
                .build();

        // Then
        assertThat(recommendation.getApplied()).isFalse();
    }

}
