package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import com.runalytics.ai_coach.repository.RecommendationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationGeneratorServiceTest {

    @Mock
    private OpenAiApiService openAiApiService;

    @Mock
    private PromptTemplateService promptTemplateService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Captor
    private ArgumentCaptor<List<Recommendation>> recommendationCaptor;

    private RecommendationGeneratorService recommendationGeneratorService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        recommendationGeneratorService = new RecommendationGeneratorService(
                openAiApiService,
                promptTemplateService,
                recommendationRepository,
                objectMapper,
                Clock.systemUTC(),
                7
        );
    }

    @Test
    void shouldGenerateRecommendationsFromReport() {
        // Given
        UUID reportId = UUID.randomUUID();
        TrainingReportDto report = TrainingReportDto.builder()
                .id(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{\"totalKm\": 50.0}")
                .markdownContent("# Training Report")
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(2)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .primaryFocus("Building aerobic base")
                .isDeloadWeek(false)
                .build();

        String systemPrompt = "You are a coach...";
        String userPrompt = "Analyze this report...";

        String aiResponse = """
                {
                  "verdict": "PARTIALLY_VALID",
                  "verdict_rationale": "Z2 work drifted into Z3",
                  "recommendations": [
                    {
                      "type": "ZONE_COMPLIANCE",
                      "priority": "HIGH",
                      "category": "ADJUSTMENT",
                      "content": "Reduce pace by 15-20 seconds per km on Z2 runs",
                      "rationale": "Current pace pushes HR into Z3 territory"
                    },
                    {
                      "type": "TRAINING_VOLUME",
                      "priority": "MEDIUM",
                      "category": "CONFIRMATION",
                      "content": "Volume progression is appropriate at 10% increase",
                      "rationale": "Following safe progression guidelines"
                    }
                  ]
                }
                """;

        when(promptTemplateService.buildSystemPrompt(context)).thenReturn(systemPrompt);
        when(promptTemplateService.buildUserPrompt(report, context)).thenReturn(userPrompt);
        when(openAiApiService.analyze(systemPrompt, userPrompt)).thenReturn(aiResponse);
        when(recommendationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        // When
        List<Recommendation> recommendations = recommendationGeneratorService
                .generateRecommendations(report, context);

        // Then
        assertThat(recommendations).hasSize(2);

        // Verify first recommendation
        Recommendation rec1 = recommendations.get(0);
        assertThat(rec1.getUserId()).isEqualTo("test-user");
        assertThat(rec1.getReportId()).isEqualTo(reportId);
        assertThat(rec1.getType().name()).isEqualTo("ZONE_COMPLIANCE");
        assertThat(rec1.getPriority().name()).isEqualTo("HIGH");
        assertThat(rec1.getContent()).contains("Reduce pace");
        assertThat(rec1.getRationale()).contains("HR into Z3");
        assertThat(rec1.getVerdict()).isEqualTo(TrainingVerdict.PARTIALLY_VALID);
        assertThat(rec1.getWeekInCycle()).isEqualTo(2);
        assertThat(rec1.getTrainingPhase()).isEqualTo(TrainingCycleContext.TrainingPhase.AEROBIC_BASE);

        // Verify second recommendation
        Recommendation rec2 = recommendations.get(1);
        assertThat(rec2.getType().name()).isEqualTo("TRAINING_VOLUME");
        assertThat(rec2.getPriority().name()).isEqualTo("MEDIUM");

        // Verify repository was called
        verify(recommendationRepository).saveAll(recommendationCaptor.capture());
        assertThat(recommendationCaptor.getValue()).hasSize(2);
    }

    @Test
    void shouldHandleInvalidAiResponse() {
        // Given
        TrainingReportDto report = TrainingReportDto.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .markdownContent("# Report")
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(1)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .build();

        when(promptTemplateService.buildSystemPrompt(any())).thenReturn("system");
        when(promptTemplateService.buildUserPrompt(any(), any())).thenReturn("user");
        when(openAiApiService.analyze(any(), any())).thenReturn("invalid json");

        // When & Then
        assertThatThrownBy(() -> recommendationGeneratorService.generateRecommendations(report, context))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate recommendations");
    }

    @Test
    void shouldHandleEmptyRecommendations() {
        // Given
        UUID reportId = UUID.randomUUID();
        TrainingReportDto report = TrainingReportDto.builder()
                .id(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .markdownContent("# Report")
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(4)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .isDeloadWeek(true)
                .build();

        String aiResponse = """
            {
              "verdict": "VALID",
              "verdict_rationale": "Perfect deload week",
              "recommendations": []
            }
            """;

        when(promptTemplateService.buildSystemPrompt(any())).thenReturn("system");
        when(promptTemplateService.buildUserPrompt(any(), any())).thenReturn("user");
        when(openAiApiService.analyze(any(), any())).thenReturn(aiResponse);
        when(recommendationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        // When
        List<Recommendation> recommendations = recommendationGeneratorService
                .generateRecommendations(report, context);

        // Then
        assertThat(recommendations).isEmpty();
        verify(recommendationRepository).saveAll(recommendationCaptor.capture());
        assertThat(recommendationCaptor.getValue()).isEmpty();
    }

    @Test
    void shouldSetExpirationDateCorrectly() {
        // Given
        UUID reportId = UUID.randomUUID();
        TrainingReportDto report = TrainingReportDto.builder()
                .id(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .markdownContent("# Report")
                .build();

        TrainingCycleContext context = TrainingCycleContext.builder()
                .weekInCycle(1)
                .phase(TrainingCycleContext.TrainingPhase.QUALITY_BLOCK)
                .build();

        String aiResponse = """
            {
              "verdict": "VALID",
              "verdict_rationale": "Good quality session",
              "recommendations": [
                {
                  "type": "RECOVERY",
                  "priority": "HIGH",
                  "category": "ADJUSTMENT",
                  "content": "Take extra rest day",
                  "rationale": "Hard session completed"
                }
              ]
            }
            """;

        when(promptTemplateService.buildSystemPrompt(any())).thenReturn("system");
        when(promptTemplateService.buildUserPrompt(any(), any())).thenReturn("user");
        when(openAiApiService.analyze(any(), any())).thenReturn(aiResponse);
        when(recommendationRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        Instant before = Instant.now();

        // When
        List<Recommendation> recommendations = recommendationGeneratorService
                .generateRecommendations(report, context);

        Instant after = Instant.now().plus(8, ChronoUnit.DAYS);

        // Then
        assertThat(recommendations).hasSize(1);
        Recommendation rec = recommendations.get(0);

        assertThat(rec.getCreatedAt()).isNotNull();
        assertThat(rec.getCreatedAt()).isBetween(before, after);

        assertThat(rec.getExpiresAt()).isNotNull();
        assertThat(rec.getExpiresAt()).isAfter(rec.getCreatedAt());

        // Should expire in approximately 7 days
        long daysBetween = ChronoUnit.DAYS.between(rec.getCreatedAt(), rec.getExpiresAt());
        assertThat(daysBetween).isEqualTo(7);
    }
}