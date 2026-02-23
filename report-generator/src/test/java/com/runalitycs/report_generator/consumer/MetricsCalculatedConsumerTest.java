package com.runalitycs.report_generator.consumer;

import com.runalitycs.report_generator.dto.ActivityMetricsDto;
import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.producer.ReportGeneratedProducer;
import com.runalitycs.report_generator.service.ReportGeneratorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MetricsCalculatedConsumerTest {

    @Mock
    private ReportGeneratorService reportGeneratorService;

    @Mock
    private ReportGeneratedProducer reportGeneratedProducer;

    @InjectMocks
    private MetricsCalculatedConsumer consumer;

    private ActivityMetricsDto activityMetrics;

    @BeforeEach
    void setUp() {
        activityMetrics = ActivityMetricsDto.builder()
                .activityId(UUID.randomUUID())
                .userId("test-user")
                .startedAt(Instant.parse("2024-12-08T10:00:00Z"))
                .totalDistance(new BigDecimal("10.5"))
                .totalDuration(3600)
                .averagePace(343)
                .averageHeartRate(145)
                .build();
    }

    @Test
    void shouldConsumeMetricsCalculatedEvent() {
        // Given
        TrainingReportDto report = TrainingReportDto.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Report")
                .summaryJson("{\"totalKm\": 52.0}")
                .createdAt(Instant.now())
                .triggerActivityId(activityMetrics.activityId())
                .athleteName("Test Runner")
                .currentGoal("Marathon sub-3:30")
                .build();

        when(reportGeneratorService.generateReport(activityMetrics))
                .thenReturn(report);

        // When
        consumer.consume(activityMetrics);

        // Then
        verify(reportGeneratorService).generateReport(activityMetrics);
        verify(reportGeneratedProducer).publish(any());
    }

    @Test
    void shouldGenerateReportOnValidMessage() {
        // Given
        TrainingReportDto expectedReport = TrainingReportDto.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Training Report - Week 49/2024")
                .summaryJson("{\"totalKm\": 52.0, \"trend\": \"improving\"}")
                .createdAt(Instant.now())
                .triggerActivityId(activityMetrics.activityId())
                .athleteName("Test Runner")
                .currentGoal("Marathon sub-3:30")
                .build();

        when(reportGeneratorService.generateReport(activityMetrics))
                .thenReturn(expectedReport);

        // When
        consumer.consume(activityMetrics);

        // Then
        verify(reportGeneratorService, times(1)).generateReport(activityMetrics);
        verify(reportGeneratedProducer, times(1)).publish(argThat(event ->
                event.reportId().equals(expectedReport.id()) &&
                        event.userId().equals(expectedReport.userId()) &&
                        event.weekNumber().equals(expectedReport.weekNumber()) &&
                        event.year().equals(expectedReport.year()) &&
                        event.summaryJson().equals(expectedReport.summaryJson())
        ));
    }

    @Test
    void shouldHandleInvalidMessage() {
        // Given - activityMetrics with null userId (invalid)
        ActivityMetricsDto invalidMetrics = ActivityMetricsDto.builder()
                .activityId(UUID.randomUUID())
                .userId(null) // Invalid
                .startedAt(Instant.now())
                .totalDistance(new BigDecimal("10.0"))
                .totalDuration(3600)
                .build();

        when(reportGeneratorService.generateReport(invalidMetrics))
                .thenThrow(new IllegalArgumentException("userId cannot be null"));

        // When & Then
        assertThatThrownBy(() -> consumer.consume(invalidMetrics))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId cannot be null");

        verify(reportGeneratorService).generateReport(invalidMetrics);
        verify(reportGeneratedProducer, never()).publish(any());
    }

    @Test
    void shouldHandleProfileNotFound() {
        // Given
        when(reportGeneratorService.generateReport(activityMetrics))
                .thenThrow(new IllegalArgumentException("Profile not found for userId: test-user"));

        // When & Then
        assertThatThrownBy(() -> consumer.consume(activityMetrics))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");

        verify(reportGeneratorService).generateReport(activityMetrics);
        verify(reportGeneratedProducer, never()).publish(any());
    }

    @Test
    void shouldPublishEventWithCorrectFields() {
        // Given
        UUID reportId = UUID.randomUUID();
        UUID activityId = activityMetrics.activityId();
        Instant createdAt = Instant.parse("2024-12-08T12:00:00Z");

        TrainingReportDto report = TrainingReportDto.builder()
                .id(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Report")
                .summaryJson("{\"totalKm\": 52.0}")
                .createdAt(createdAt)
                .triggerActivityId(activityId)
                .athleteName("Test Runner")
                .currentGoal("Marathon sub-3:30")
                .build();

        when(reportGeneratorService.generateReport(activityMetrics))
                .thenReturn(report);

        // When
        consumer.consume(activityMetrics);

        // Then
        verify(reportGeneratedProducer).publish(argThat(event -> {
            assertThat(event.reportId()).isEqualTo(reportId);
            assertThat(event.userId()).isEqualTo("test-user");
            assertThat(event.weekNumber()).isEqualTo(49);
            assertThat(event.year()).isEqualTo(2024);
            assertThat(event.summaryJson()).isEqualTo("{\"totalKm\": 52.0}");
            assertThat(event.generatedAt()).isEqualTo(createdAt);
            assertThat(event.triggerActivityId()).isEqualTo(activityId);
            return true;
        }));
    }
}
