package com.runalitycs.report_generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.dto.ActivityMetricsDto;
import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.entity.TrainingReport;
import com.runalitycs.report_generator.mapper.TrainingReportMapper;
import com.runalitycs.report_generator.repository.TrainingReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class ReportGeneratorServiceTest {

    @Mock
    private AthleteProfileService athleteProfileService;

    @Mock
    private WeeklyAggregationService weeklyAggregationService;

    @Mock
    private MarkdownTemplateService markdownTemplateService;

    @Mock
    private TrainingReportRepository trainingReportRepository;

    @Mock
    private TrainingReportMapper trainingReportMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportGeneratorService reportGeneratorService;

    private ActivityMetricsDto activityMetricsDto;
    private AthleteProfile athleteProfile;
    private List<WeeklyStats> weeklyStats;
    private String generatedMarkdown;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        activityMetricsDto = ActivityMetricsDto.builder()
                .activityId(UUID.randomUUID())
                .userId("test-user")
                .startedAt(Instant.parse("2024-12-08T10:00:00Z")) // Week 49, 2024
                .totalDistance(new BigDecimal("10.5"))
                .totalDuration(3600)
                .averagePace(343)
                .averageHeartRate(145)
                .averageCadence(170)
                .build();

        athleteProfile = AthleteProfile.builder()
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .currentGoal("Marathon sub-3:30")
                .build();

        WeeklyStats currentWeek = WeeklyStats.builder()
                .weekNumber(49)
                .year(2024)
                .totalActivities(4)
                .totalDistance(new BigDecimal("52.0"))
                .totalDuration(15600)
                .averagePace(300)
                .averageHeartRate(145)
                .build();

        weeklyStats = List.of(currentWeek);

        generatedMarkdown = "# Training Report - Week 49/2024\n**Athlete**: Test Runner\n";

        /*
        me queda por hacer :
        que estos tests pasen, tests de los dtos añadidos, test del training report mapper, test de activityMetrics y WeeklyStats ?
         */
    }

    @Test
    void shouldGenerateReportWithValidActivity() throws JsonProcessingException {
        // Given
        when(athleteProfileService.getProfileByUserId("test-user"))
                .thenReturn(athleteProfile);

        when(weeklyAggregationService.getWeeklyStats("test-user", 4))
                .thenReturn(weeklyStats);

        when(markdownTemplateService.generateWeeklyReport(
                eq(athleteProfile),
                any(WeeklyStats.class),
                eq(weeklyStats)
        )).thenReturn(generatedMarkdown);

        TrainingReport savedReport = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent(generatedMarkdown)
                .summaryJson("{\"totalKm\":52.0}")
                .triggerActivityId(activityMetricsDto.activityId())
                .build();

        when(trainingReportRepository.save(any(TrainingReport.class)))
                .thenReturn(savedReport);

        TrainingReportDto expectedDto = new TrainingReportDto(
                savedReport.getId(),
                savedReport.getUserId(),
                savedReport.getWeekNumber(),
                savedReport.getYear(),
                savedReport.getMarkdownContent(),
                savedReport.getSummaryJson(),
                savedReport.getCreatedAt(),
                savedReport.getTriggerActivityId()
        );

        when(trainingReportMapper.toDto(any(TrainingReport.class)))
                .thenReturn(expectedDto);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"totalKm\":52.0}");

        // When
        TrainingReportDto result = reportGeneratorService.generateReport(activityMetricsDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo("test-user");
        assertThat(result.weekNumber()).isEqualTo(49);
        assertThat(result.year()).isEqualTo(2024);
        assertThat(result.markdownContent()).isEqualTo(generatedMarkdown);
        assertThat(result.triggerActivityId()).isEqualTo(activityMetricsDto.activityId());

        verify(athleteProfileService).getProfileByUserId("test-user");
        verify(weeklyAggregationService).getWeeklyStats("test-user", 4);
        verify(markdownTemplateService).generateWeeklyReport(any(), any(), any());
        verify(trainingReportRepository).save(any(TrainingReport.class));
    }

    @Test
    void shouldThrowExceptionWhenProfileNotFound() {
        // Given
        when(athleteProfileService.getProfileByUserId("test-user"))
                .thenThrow(new IllegalArgumentException("Profile not found for userId: test-user"));

        // When & Then
        assertThatThrownBy(() -> reportGeneratorService.generateReport(activityMetricsDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");

        verify(athleteProfileService).getProfileByUserId("test-user");
        verify(weeklyAggregationService, never()).getWeeklyStats(anyString(), anyInt());
        verify(trainingReportRepository, never()).save(any());
    }

    @Test
    void shouldRegenerateReportForExistingWeek() throws JsonProcessingException {
        // Given - Report already exists
        TrainingReport existingReport = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("Old content")
                .summaryJson("{}")
                .build();

        when(athleteProfileService.getProfileByUserId("test-user"))
                .thenReturn(athleteProfile);

        when(trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 49, 2024))
                .thenReturn(Optional.of(existingReport));

        when(weeklyAggregationService.getWeeklyStats("test-user", 4))
                .thenReturn(weeklyStats);

        when(markdownTemplateService.generateWeeklyReport(any(), any(), any()))
                .thenReturn(generatedMarkdown);

        TrainingReport newReport = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent(generatedMarkdown)
                .summaryJson("{\"totalKm\":52.0}")
                .build();

        when(trainingReportRepository.save(any(TrainingReport.class)))
                .thenReturn(newReport);

        TrainingReportDto expectedDto = new TrainingReportDto(
                newReport.getId(),
                newReport.getUserId(),
                newReport.getWeekNumber(),
                newReport.getYear(),
                newReport.getMarkdownContent(),
                newReport.getSummaryJson(),
                newReport.getCreatedAt(),
                newReport.getTriggerActivityId()
        );

        when(trainingReportMapper.toDto(any(TrainingReport.class)))
                .thenReturn(expectedDto);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"totalKm\":52.0}");

        // When
        TrainingReportDto result = reportGeneratorService.generateReport(activityMetricsDto);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.markdownContent()).isEqualTo(generatedMarkdown);

        verify(trainingReportRepository).delete(existingReport); // Old report deleted
        verify(trainingReportRepository).save(any(TrainingReport.class)); // New report saved
    }

    @Test
    void shouldCreateSummaryJson() throws Exception {
        // Given
        when(athleteProfileService.getProfileByUserId("test-user"))
                .thenReturn(athleteProfile);

        WeeklyStats statsWithTrend = WeeklyStats.builder()
                .weekNumber(49)
                .year(2024)
                .totalActivities(4)
                .totalDistance(new BigDecimal("52.0"))
                .totalDuration(15600)
                .averagePace(300)
                .trend("improving")
                .build();

        when(weeklyAggregationService.getWeeklyStats("test-user", 4))
                .thenReturn(List.of(statsWithTrend));

        when(markdownTemplateService.generateWeeklyReport(any(), any(), any()))
                .thenReturn(generatedMarkdown);

        TrainingReport savedReport = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent(generatedMarkdown)
                .summaryJson("{\"weekNumber\":49,\"year\":2024,\"totalActivities\":4,\"totalKm\":52.0,\"totalDuration\":15600,\"averagePace\":300,\"trend\":\"improving\"}")
                .build();

        when(trainingReportRepository.save(any(TrainingReport.class)))
                .thenReturn(savedReport);

        TrainingReportDto expectedDto = new TrainingReportDto(
                savedReport.getId(),
                savedReport.getUserId(),
                savedReport.getWeekNumber(),
                savedReport.getYear(),
                savedReport.getMarkdownContent(),
                savedReport.getSummaryJson(),
                savedReport.getCreatedAt(),
                savedReport.getTriggerActivityId()
        );

        when(trainingReportMapper.toDto(any(TrainingReport.class)))
                .thenReturn(expectedDto);

        when(objectMapper.writeValueAsString(any()))
                .thenReturn("{\"weekNumber\":49,\"year\":2024,\"totalActivities\":4,\"totalKm\":52.0,\"totalDuration\":15600,\"averagePace\":300,\"trend\":\"improving\"}");

        // When
        TrainingReportDto result = reportGeneratorService.generateReport(activityMetricsDto);

        // Then
        assertThat(result.summaryJson()).isNotNull();
        assertThat(result.summaryJson()).contains("\"weekNumber\":49");
        assertThat(result.summaryJson()).contains("\"totalKm\":52");
        assertThat(result.summaryJson()).contains("\"trend\":\"improving\"");
    }

}
