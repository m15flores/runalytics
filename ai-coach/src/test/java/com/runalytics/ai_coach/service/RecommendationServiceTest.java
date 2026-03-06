package com.runalytics.ai_coach.service;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.repository.RecommendationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository repository;

    @Mock
    private Clock clock;

    @InjectMocks
    private RecommendationService service;

    private static final Instant FIXED_NOW = Instant.parse("2026-03-06T10:00:00Z");

    @Test
    void shouldReturnActiveRecommendationsForUser() {
        // Given
        String userId = "mario-runner";
        when(clock.instant()).thenReturn(FIXED_NOW);

        List<Recommendation> recommendations = List.of(
                Recommendation.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .type(RecommendationType.ZONE_COMPLIANCE)
                        .priority(Priority.HIGH)
                        .content("You ran in Z3 for 40% of the session")
                        .rationale("Z2 compliance is non-negotiable during aerobic base")
                        .verdict(TrainingVerdict.INVALID)
                        .weekInCycle(2)
                        .createdAt(FIXED_NOW.minusSeconds(3600))
                        .build(),
                Recommendation.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .type(RecommendationType.CARDIAC_DRIFT)
                        .priority(Priority.MEDIUM)
                        .content("Cardiac drift of 12bpm detected")
                        .rationale("Moderate drift — aerobic efficiency improving")
                        .verdict(TrainingVerdict.PARTIALLY_VALID)
                        .weekInCycle(2)
                        .createdAt(FIXED_NOW.minusSeconds(3600))
                        .build()
        );
        when(repository.findActiveRecommendationsByUserId(eq(userId), any(Instant.class)))
                .thenReturn(recommendations);

        // When
        List<Recommendation> result = service.getActiveRecommendations(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(RecommendationType.ZONE_COMPLIANCE);
        assertThat(result.get(0).getPriority()).isEqualTo(Priority.HIGH);
        assertThat(result.get(1).getType()).isEqualTo(RecommendationType.CARDIAC_DRIFT);
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveRecommendations() {
        // Given
        String userId = "new-user";
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(repository.findActiveRecommendationsByUserId(eq(userId), any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<Recommendation> result = service.getActiveRecommendations(userId);

        // Then
        assertThat(result).isEmpty();
    }
}
