package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.entity.WeeklyStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkdownTemplateServiceTest {
    private MarkdownTemplateService markdownTemplateService;
    private AthleteProfile profile;
    private WeeklyStats currentWeek;
    private List<WeeklyStats> last4Weeks;

    @BeforeEach
    void setup() {
        markdownTemplateService = new MarkdownTemplateService();

        profile = AthleteProfile.builder()
                .userId("test-runner")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .currentGoal("Marathon sub-3:30")
                .build();

        currentWeek = WeeklyStats.builder()
                .weekNumber(49)
                .year(2024)
                .totalActivities(4)
                .totalDistance(new BigDecimal("52.0"))
                .totalDuration(15600) // 4h 20m
                .totalCalories(3200)
                .averagePace(300) // 5:00 min/km
                .averageHeartRate(145)
                .averageCadence(170)
                .totalAscent(200)
                .totalDescent(200)
                .build();

        last4Weeks = Arrays.asList(currentWeek);
    }

    @Test
    void shouldGenerateBasicWeeklyReport() {
        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).isNotNull();
        assertThat(markdown).isNotEmpty();
        assertThat(markdown).contains("# Training Report - Week 49/2024");
        assertThat(markdown).contains("**Athlete**: Test Runner");
    }

    @Test
    void shouldIncludeWeeklySummary() {
        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("## This Week Summary");
        assertThat(markdown).contains("Total Activities: 4");
        assertThat(markdown).contains("Total Distance: 52.0 km");
        assertThat(markdown).contains("Total Duration: 4h 20m");
        assertThat(markdown).contains("Average Pace: 5:00 min/km");
        assertThat(markdown).contains("Average Heart Rate: 145 bpm");
        assertThat(markdown).contains("Average Cadence: 170 spm");
    }

    @Test
    void shouldIncludeComparison4Weeks() {
        // Given - 4 weeks of data
        WeeklyStats week48 = WeeklyStats.builder()
                .weekNumber(48).year(2024)
                .totalActivities(4).totalDistance(new BigDecimal("48.0"))
                .totalDuration(14400).averagePace(300)
                .build();

        WeeklyStats week47 = WeeklyStats.builder()
                .weekNumber(47).year(2024)
                .totalActivities(3).totalDistance(new BigDecimal("45.0"))
                .totalDuration(13500).averagePace(300)
                .build();

        WeeklyStats week46 = WeeklyStats.builder()
                .weekNumber(46).year(2024)
                .totalActivities(3).totalDistance(new BigDecimal("42.0"))
                .totalDuration(12600).averagePace(300)
                .build();

        last4Weeks = Arrays.asList(currentWeek, week48, week47, week46);

        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("## Last 4 Weeks Comparison");
        assertThat(markdown).contains("| Week | Activities | Distance | Duration | Avg Pace |");
        assertThat(markdown).contains("| 49/2024 | 4 | 52.0 km | 4h 20m | 5:00 min/km |");
        assertThat(markdown).contains("| 48/2024 | 4 | 48.0 km | 4h 00m | 5:00 min/km |");
    }

    @Test
    void shouldIncludeDetailedAnalysis() {
        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("## Detailed Analysis");
        assertThat(markdown).contains("**Volume**:");
        assertThat(markdown).contains("**Pace**:");
        assertThat(markdown).contains("**Cadence**:");
    }

    @Test
    void shouldIncludeHrZonesDistribution() {
        // Given - Add HR zones to current week
        Map<String, Integer> hrZones = new HashMap<>();
        hrZones.put("Z1", 600);   // 10 minutes
        hrZones.put("Z2", 10800); // 3 hours
        hrZones.put("Z3", 4200);  // 70 minutes
        currentWeek.setHrZonesDistribution(hrZones);

        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("## Heart Rate Zones Distribution");
        assertThat(markdown).contains("| Zone | Time | Percentage |");
        assertThat(markdown).contains("| Z1 |");
        assertThat(markdown).contains("| Z2 |");
        assertThat(markdown).contains("| Z3 |");
    }

    @Test
    void shouldIncludeGoalsSection() {
        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("## Goals");
    }

    @Test
    void shouldFormatPaceCorrectly() {
        // Given
        currentWeek.setAveragePace(305); // 5:05 min/km

        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("5:05 min/km");
    }

    @Test
    void shouldFormatDurationCorrectly() {
        // Given
        currentWeek.setTotalDuration(7384); // 2h 3m 4s → should show as 2h 03m

        // When
        String markdown = markdownTemplateService.generateWeeklyReport(profile, currentWeek, last4Weeks);

        // Then
        assertThat(markdown).contains("2h 03m");
    }

    @Test
    void shouldHandleMissingOptionalData() {
        // Given - Week with minimal data
        WeeklyStats minimalWeek = WeeklyStats.builder()
                .weekNumber(50)
                .year(2024)
                .totalActivities(1)
                .totalDistance(new BigDecimal("10.0"))
                .totalDuration(3600)
                .build(); // No pace, HR, cadence, zones

        AthleteProfile minimalProfile = AthleteProfile.builder()
                .userId("minimal-user")
                .name("Minimal")
                .build(); // No goal

        // When
        String markdown = markdownTemplateService.generateWeeklyReport(
                minimalProfile,
                minimalWeek,
                List.of(minimalWeek)
        );

        // Then - Should not crash and should handle nulls gracefully
        assertThat(markdown).isNotNull();
        assertThat(markdown).contains("# Training Report - Week 50/2024");
        assertThat(markdown).contains("**Athlete**: Minimal");
        assertThat(markdown).contains("Total Activities: 1");
        assertThat(markdown).doesNotContain("Average Pace:"); // Should skip null fields
        assertThat(markdown).doesNotContain("Average Heart Rate:");
        assertThat(markdown).doesNotContain("Heart Rate Zones Distribution");
    }

}
