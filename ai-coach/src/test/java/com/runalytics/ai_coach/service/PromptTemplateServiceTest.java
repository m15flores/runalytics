package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptTemplateServiceTest {

    private PromptTemplateService promptTemplateService;
    private TrainingCycleContext aerobicBaseContext;

    @BeforeEach
    void setUp() {
        promptTemplateService = new PromptTemplateService(new ObjectMapper());

        // Default context: Aerobic base, week 2 of cycle
        aerobicBaseContext = TrainingCycleContext.builder()
                .weekInCycle(2)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .primaryFocus("Building aerobic base with Z2 work")
                .isDeloadWeek(false)
                .targetZone("Zone 2")
                .build();
    }

    @Test
    void shouldBuildSystemPromptWithAerobicBasePhilosophy() {
        // When
        String systemPrompt = promptTemplateService.buildSystemPrompt(aerobicBaseContext);

        // Then
        assertThat(systemPrompt).isNotNull();
        assertThat(systemPrompt).isNotEmpty();

        // Should contain coaching philosophy
        assertThat(systemPrompt).containsIgnoringCase("Aerobic Base Development");
        assertThat(systemPrompt).containsIgnoringCase("Zone 2 compliance");
        assertThat(systemPrompt).containsIgnoringCase("Week 2 of 4-week cycle");

        // Should contain physiological hierarchy
        assertThat(systemPrompt).containsIgnoringCase("Zone compliance");
        assertThat(systemPrompt).containsIgnoringCase("Cardiac drift");
        assertThat(systemPrompt).containsIgnoringCase("Pace (a CONSEQUENCE");

        // Should authorize invalidation
        assertThat(systemPrompt).containsIgnoringCase("INVALIDATE");
        assertThat(systemPrompt).containsIgnoringCase("verdict");

        // Should be direct, not diplomatic
        assertThat(systemPrompt).containsIgnoringCase("Be direct");
        assertThat(systemPrompt).containsIgnoringCase("corrective");
    }

    @Test
    void shouldBuildSystemPromptForDeloadWeek() {
        // Given - Week 4 deload
        TrainingCycleContext deloadContext = TrainingCycleContext.builder()
                .weekInCycle(4)
                .phase(TrainingCycleContext.TrainingPhase.AEROBIC_BASE)
                .primaryFocus("Recovery and adaptation")
                .isDeloadWeek(true)
                .build();

        // When
        String systemPrompt = promptTemplateService.buildSystemPrompt(deloadContext);

        // Then
        assertThat(systemPrompt).contains("DELOAD WEEK");
        assertThat(systemPrompt).contains("Week 4 of 4-week cycle");
    }

    @Test
    void shouldBuildUserPromptWithCycleContext() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 52.5,
                  "totalDuration": 14400,
                  "averagePace": 305,
                  "averageHeartRate": 145,
                  "totalActivities": 5
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(49)
                .year(2024)
                .summaryJson(summaryJson)
                .athleteName("John Doe")
                .currentGoal("Marathon sub-3:30")
                .build();

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).contains("Week 49/2024 (Week 2 of current 4-week cycle)");
        assertThat(userPrompt).contains("Phase: AEROBIC_BASE");
        assertThat(userPrompt).contains("Deload week: NO");
        assertThat(userPrompt).contains("Building aerobic base");

        // Should contain critical evaluation section
        assertThat(userPrompt).contains("CRITICAL EVALUATION REQUIRED");
        assertThat(userPrompt).contains("Zone Compliance");
        assertThat(userPrompt).contains("Cardiac Drift");
    }

    @Test
    void shouldFormatDurationCorrectly() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 10.0,
                  "totalDuration": 7380,
                  "averagePace": 300,
                  "averageHeartRate": 140,
                  "totalActivities": 1
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(50)
                .year(2024)
                .summaryJson(summaryJson)
                .athleteName("Test Runner")
                .currentGoal("Improve endurance")
                .build();

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).contains("2h 03m");
    }

    @Test
    void shouldFormatPaceCorrectly() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 10.0,
                  "totalDuration": 3600,
                  "averagePace": 305,
                  "averageHeartRate": 140,
                  "totalActivities": 1
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(50)
                .year(2024)
                .summaryJson(summaryJson)
                .athleteName("Test Runner")
                .currentGoal("Run faster")
                .build();

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).contains("5:05 min/km");
    }

    @Test
    void shouldHandleMissingOptionalData() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 0,
                  "totalDuration": 0,
                  "totalActivities": 0
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(1)
                .year(2025)
                .summaryJson(summaryJson)
                .athleteName("New Runner")
                .build(); // currentGoal not set → null → "Not specified"

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).isNotNull();
        assertThat(userPrompt).contains("Week 1/2025");
        assertThat(userPrompt).contains("0.0 km");
        assertThat(userPrompt).contains("Not specified");
    }

    @Test
    void shouldUseAthleteNameFromStructuredField() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 50.0,
                  "totalDuration": 10800,
                  "averagePace": 360,
                  "totalActivities": 4
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(49)
                .year(2024)
                .summaryJson(summaryJson)
                .athleteName("Jane Smith")
                .currentGoal("Finish 10K")
                .build();

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).contains("Jane Smith");
    }

    @Test
    void shouldUseCurrentGoalFromStructuredField() {
        // Given
        String summaryJson = """
                {
                  "totalKm": 60.0,
                  "totalDuration": 14400,
                  "averagePace": 300,
                  "totalActivities": 5
                }
                """;

        TrainingReportDto report = TrainingReportDto.builder()
                .weekNumber(50)
                .year(2024)
                .summaryJson(summaryJson)
                .athleteName("Bob Runner")
                .currentGoal("Ultra Marathon 100km in 10 hours")
                .build();

        // When
        String userPrompt = promptTemplateService.buildUserPrompt(report, aerobicBaseContext);

        // Then
        assertThat(userPrompt).contains("Ultra Marathon 100km in 10 hours");
    }
}