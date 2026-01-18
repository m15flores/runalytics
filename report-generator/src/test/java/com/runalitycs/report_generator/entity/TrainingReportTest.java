package com.runalitycs.report_generator.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TrainingReportTest {

    @Test
    void shouldCreateTrainingReportWithRequiredFields() {
        // Given
        TrainingReport report = new TrainingReport();
        report.setUserId("user-12345");
        report.setWeekNumber(49);
        report.setYear(2024);
        report.setMarkdownContent("# Training Report");

        // When & Then
        assertNotNull(report);
        assertEquals("user-12345", report.getUserId());
        assertEquals(49, report.getWeekNumber());
        assertEquals(2024, report.getYear());
        assertEquals("# Training Report", report.getMarkdownContent());
        assertNull(report.getId()); // Not persisted yet
    }

    @Test
    void shouldCreateTrainingReportWithBuilder() {
        // Given & When
        UUID activityId = UUID.randomUUID();
        TrainingReport report = TrainingReport.builder()
                .userId("mario-runner")
                .weekNumber(48)
                .year(2024)
                .markdownContent("# Week 48 Report")
                .summaryJson("{\"totalKm\": 52.0}")
                .triggerActivityId(activityId)
                .build();

        // Then
        assertEquals("mario-runner", report.getUserId());
        assertEquals(48, report.getWeekNumber());
        assertEquals(2024, report.getYear());
        assertEquals("# Week 48 Report", report.getMarkdownContent());
        assertEquals("{\"totalKm\": 52.0}", report.getSummaryJson());
        assertEquals(activityId, report.getTriggerActivityId());
    }

    @Test
    void shouldSetOptionalFields() {
        // Given
        TrainingReport report = new TrainingReport();
        report.setUserId("user-12345");
        report.setWeekNumber(50);
        report.setYear(2024);
        report.setMarkdownContent("# Report");
        report.setSummaryJson("{\"activities\": 4}");
        report.setTriggerActivityId(UUID.randomUUID());

        // Then
        assertNotNull(report.getSummaryJson());
        assertNotNull(report.getTriggerActivityId());
    }

    @Test
    void shouldInitializeCreatedAtOnPrePersist() {
        // Given
        TrainingReport report = new TrainingReport();
        report.setUserId("user-12345");
        report.setWeekNumber(49);
        report.setYear(2024);
        report.setMarkdownContent("# Report");

        // When
        report.onCreate(); // Simulate @PrePersist

        // Then
        assertNotNull(report.getCreatedAt());
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        TrainingReport report = TrainingReport.builder()
                .userId("minimal-user")
                .weekNumber(1)
                .year(2024)
                .markdownContent("# Minimal Report")
                .build();

        // Then
        assertNotNull(report.getUserId());
        assertNotNull(report.getWeekNumber());
        assertNotNull(report.getYear());
        assertNotNull(report.getMarkdownContent());
        assertNull(report.getSummaryJson());
        assertNull(report.getTriggerActivityId());
    }

    @Test
    void shouldValidateWeekNumberRange() {
        // Given
        TrainingReport report = TrainingReport.builder()
                .userId("user-12345")
                .weekNumber(53) // Max valid week
                .year(2024)
                .markdownContent("# Week 53")
                .build();

        // Then
        assertEquals(53, report.getWeekNumber());
        assertTrue(report.getWeekNumber() >= 1 && report.getWeekNumber() <= 53);
    }

    @Test
    void shouldStoreMarkdownContentAsText() {
        // Given
        String longMarkdown = """
            # Training Report - Week 49/2024
            
            ## Summary
            - Total Distance: 52.0 km
            - Total Duration: 4h 20m
            - Total Activities: 4
            - Average Pace: 5:50 min/km
            - Average Heart Rate: 145 bpm
            
            ## Last 4 Weeks Comparison
            Week 46: 48km
            Week 47: 50km
            Week 48: 51km
            Week 49: 52km
            
            ## Detailed Analysis
            Great week with consistent training and good progression.
            Zone 2 dominance at 65% is excellent for base building.
            """;

        TrainingReport report = TrainingReport.builder()
                .userId("user-12345")
                .weekNumber(49)
                .year(2024)
                .markdownContent(longMarkdown)
                .build();

        // Then
        assertTrue(report.getMarkdownContent().length() > 255,
                "Markdown should be longer than VARCHAR(255), actual: " +
                        report.getMarkdownContent().length());
        assertTrue(report.getMarkdownContent().contains("Training Report"));
    }
}