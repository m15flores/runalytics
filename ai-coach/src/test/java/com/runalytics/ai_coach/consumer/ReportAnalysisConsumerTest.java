package com.runalytics.ai_coach.consumer;

import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import com.runalytics.ai_coach.dto.TrainingReportEventDto;
import com.runalytics.ai_coach.producer.RecommendationGeneratedProducer;
import com.runalytics.ai_coach.service.RecommendationGeneratorService;
import com.runalytics.ai_coach.service.TrainingCycleContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportAnalysisConsumerTest {

    @Mock
    private RecommendationGeneratorService recommendationGeneratorService;

    @Mock
    private TrainingCycleContextService trainingCycleContextService;

    @Mock
    private RecommendationGeneratedProducer recommendationProducer;

    @Captor
    private ArgumentCaptor<TrainingReportDto> reportCaptor;

    @Captor
    private ArgumentCaptor<TrainingCycleContext> contextCaptor;

    private ReportAnalysisConsumer reportAnalysisConsumer;

    @BeforeEach
    void setUp() {
        reportAnalysisConsumer = new ReportAnalysisConsumer(
                recommendationGeneratorService,
                trainingCycleContextService,
                recommendationProducer
        );
    }

    @Test
    void shouldConsumeReportGeneratedEvent() {
        // Given
        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{\"totalKm\": 50.0}")
                .generatedAt(Instant.now())
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(1)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .build();

        when(trainingCycleContextService.determineContext("test-user", 49))
                .thenReturn(context);
        when(recommendationGeneratorService.generateRecommendations(any(), any()))
                .thenReturn(List.of());

        // When
        reportAnalysisConsumer.consume(event);

        // Then
        verify(trainingCycleContextService).determineContext("test-user", 49);
        verify(recommendationGeneratorService).generateRecommendations(
                reportCaptor.capture(),
                contextCaptor.capture()
        );

        TrainingReportDto capturedReport = reportCaptor.getValue();
        assertThat(capturedReport.getId()).isEqualTo(reportId);
        assertThat(capturedReport.getUserId()).isEqualTo("test-user");
        assertThat(capturedReport.getWeekNumber()).isEqualTo(49);

        TrainingCycleContext capturedContext = contextCaptor.getValue();
        assertThat(capturedContext.getWeekInCycle()).isEqualTo(1);
    }

    @Test
    void shouldPublishRecommendationsAfterGeneration() {
        // Given
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(50)
                .year(2024)
                .summaryJson("{}")
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(2)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .build();

        Recommendation rec = Recommendation.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .build();

        when(trainingCycleContextService.determineContext(any(), any())).thenReturn(context);
        when(recommendationGeneratorService.generateRecommendations(any(), any()))
                .thenReturn(List.of(rec));

        // When
        reportAnalysisConsumer.consume(event);

        // Then
        verify(recommendationProducer).publishRecommendations(eq(event), anyList());
    }

    @Test
    void shouldHandleErrorGracefully() {
        // Given
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(51)
                .year(2024)
                .summaryJson("{}")
                .build();

        when(trainingCycleContextService.determineContext(any(), any()))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then - should not throw
        reportAnalysisConsumer.consume(event);

        // Should not have called generator
        verify(recommendationGeneratorService, never()).generateRecommendations(any(), any());
        verify(recommendationProducer, never()).publishRecommendations(any(), any());
    }
}
