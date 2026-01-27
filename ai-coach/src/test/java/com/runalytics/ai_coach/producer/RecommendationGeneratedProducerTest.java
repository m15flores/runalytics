package com.runalytics.ai_coach.producer;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.dto.RecommendationGeneratedEventDto;
import com.runalytics.ai_coach.dto.TrainingReportEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationGeneratedProducerTest {

    @Mock
    private KafkaTemplate<String, RecommendationGeneratedEventDto> kafkaTemplate;

    @Captor
    private ArgumentCaptor<RecommendationGeneratedEventDto> eventCaptor;

    private RecommendationGeneratedProducer producer;

    private static final String TOPIC = "recommendations.generated";

    @BeforeEach
    void setUp() {
        producer = new RecommendationGeneratedProducer(kafkaTemplate, TOPIC);
    }

    @Test
    void shouldPublishRecommendationGeneratedEvent() {
        // Given
        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto reportEvent = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .generatedAt(Instant.now())
                .build();

        Recommendation rec = Recommendation.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .reportId(reportId)
                .type(RecommendationType.ZONE_COMPLIANCE)
                .priority(Priority.HIGH)
                .content("Fix your Z2")
                .rationale("Drifting to Z3")
                .verdict(TrainingVerdict.PARTIALLY_VALID)
                .build();

        // When
        producer.publishRecommendations(reportEvent, List.of(rec));

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq("test-user"), eventCaptor.capture());

        RecommendationGeneratedEventDto event = eventCaptor.getValue();
        assertThat(event.getReportId()).isEqualTo(reportId);
        assertThat(event.getUserId()).isEqualTo("test-user");
        assertThat(event.getWeekNumber()).isEqualTo(49);
        assertThat(event.getYear()).isEqualTo(2024);
        assertThat(event.getVerdict()).isEqualTo(TrainingVerdict.PARTIALLY_VALID);
        assertThat(event.getRecommendations()).hasSize(1);
    }

    @Test
    void shouldUseUserIdAsKey() {
        // Given
        TrainingReportEventDto reportEvent = TrainingReportEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("athlete-123")
                .weekNumber(1)
                .year(2025)
                .summaryJson("{}")
                .build();

        // When
        producer.publishRecommendations(reportEvent, List.of());

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq("athlete-123"), any());
    }

    @Test
    void shouldIncludeAllRecommendationDetails() {
        // Given
        UUID rec1Id = UUID.randomUUID();
        UUID rec2Id = UUID.randomUUID();
        UUID reportId = UUID.randomUUID();

        TrainingReportEventDto reportEvent = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user")
                .weekNumber(8)
                .year(2024)
                .summaryJson("{}")
                .build();

        Recommendation rec1 = Recommendation.builder()
                .id(rec1Id)
                .type(RecommendationType.CARDIAC_DRIFT)
                .priority(Priority.HIGH)
                .content("Slow down")
                .metadata("{\"category\": \"RISK\"}")
                .verdict(TrainingVerdict.INVALID)
                .build();

        Recommendation rec2 = Recommendation.builder()
                .id(rec2Id)
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Rest more")
                .metadata("{\"category\": \"ADJUSTMENT\"}")
                .verdict(TrainingVerdict.INVALID)
                .build();

        // When
        producer.publishRecommendations(reportEvent, List.of(rec1, rec2));

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq("test-user"), eventCaptor.capture());

        RecommendationGeneratedEventDto event = eventCaptor.getValue();
        assertThat(event.getRecommendations()).hasSize(2);

        RecommendationGeneratedEventDto.RecommendationSummary summary1 = event.getRecommendations().get(0);
        assertThat(summary1.getRecommendationId()).isEqualTo(rec1Id);
        assertThat(summary1.getType()).isEqualTo(RecommendationType.CARDIAC_DRIFT);
        assertThat(summary1.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(summary1.getCategory()).isEqualTo("RISK");
        assertThat(summary1.getContent()).isEqualTo("Slow down");
    }

    @Test
    void shouldHandleEmptyRecommendations() {
        // Given
        TrainingReportEventDto reportEvent = TrainingReportEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(4)
                .year(2024)
                .summaryJson("{}")
                .build();

        // When
        producer.publishRecommendations(reportEvent, List.of());

        // Then
        verify(kafkaTemplate).send(eq(TOPIC), eq("test-user"), eventCaptor.capture());

        RecommendationGeneratedEventDto event = eventCaptor.getValue();
        assertThat(event.getRecommendations()).isEmpty();
    }
}
