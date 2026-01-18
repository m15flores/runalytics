package com.runalitycs.report_generator.service;


import com.runalitycs.report_generator.entity.ActivityMetrics;
import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.repository.ActivityMetricsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklyAggregationServiceTest {

    @Mock
    private ActivityMetricsRepository activityMetricsRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private WeeklyAggregationService service;

    private Instant baseTime;
    private String userId;

    @BeforeEach
    void setUp() {
        baseTime = Instant.parse("2024-12-08T10:00:00Z"); // Sunday of week 49
        userId = "test-user";

        // Mock clock to return our fixed date
        Clock fixedClock = Clock.fixed(baseTime, ZoneId.of("Europe/Paris"));
        when(clock.instant()).thenReturn(fixedClock.instant());
        when(clock.getZone()).thenReturn(fixedClock.getZone());
    }

    @Test
    void shouldAggregateWeeklyStatsForSingleWeek() {
        // Given - 3 activities in the same week
        List<ActivityMetrics> activities = Arrays.asList(
                createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170),
                createActivityMetric(baseTime.minusSeconds(86400), 12.0, 4320, 360, 150, 172),
                createActivityMetric(baseTime.minusSeconds(172800), 8.0, 2880, 360, 140, 168)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        assertThat(stats).hasSize(1);

        WeeklyStats weekStats = stats.get(0);
        assertThat(weekStats.getWeekNumber()).isEqualTo(49);
        assertThat(weekStats.getYear()).isEqualTo(2024);
        assertThat(weekStats.getTotalActivities()).isEqualTo(3);
        assertThat(weekStats.getTotalDistance()).isEqualByComparingTo(new BigDecimal("30.0"));
        assertThat(weekStats.getTotalDuration()).isEqualTo(10800); // 3600 + 4320 + 2880
    }

    @Test
    void shouldAggregateMultipleWeeks() {
        // Given - Activities spread across 3 weeks
        Instant week1 = Instant.parse("2024-12-08T10:00:00Z"); // Week 49
        Instant week2 = week1.minusSeconds(7 * 86400); // Week 48
        Instant week3 = week2.minusSeconds(7 * 86400); // Week 47

        List<ActivityMetrics> activities = Arrays.asList(
                // Week 49: 2 activities
                createActivityMetric(week1, 10.0, 3600, 360, 145, 170),
                createActivityMetric(week1.minusSeconds(86400), 12.0, 4320, 360, 150, 172),

                // Week 48: 1 activity
                createActivityMetric(week2, 15.0, 5400, 360, 148, 171),

                // Week 47: 3 activities
                createActivityMetric(week3, 8.0, 2880, 360, 140, 168),
                createActivityMetric(week3.minusSeconds(86400), 10.0, 3600, 360, 143, 169),
                createActivityMetric(week3.minusSeconds(172800), 9.0, 3240, 360, 142, 170)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 3);

        // Then
        assertThat(stats).hasSize(3);

        // Week 49 (most recent)
        WeeklyStats week49 = stats.get(0);
        assertThat(week49.getWeekNumber()).isEqualTo(49);
        assertThat(week49.getTotalActivities()).isEqualTo(2);
        assertThat(week49.getTotalDistance()).isEqualByComparingTo(new BigDecimal("22.0"));

        // Week 48
        WeeklyStats week48 = stats.get(1);
        assertThat(week48.getWeekNumber()).isEqualTo(48);
        assertThat(week48.getTotalActivities()).isEqualTo(1);
        assertThat(week48.getTotalDistance()).isEqualByComparingTo(new BigDecimal("15.0"));

        // Week 47
        WeeklyStats week47 = stats.get(2);
        assertThat(week47.getWeekNumber()).isEqualTo(47);
        assertThat(week47.getTotalActivities()).isEqualTo(3);
        assertThat(week47.getTotalDistance()).isEqualByComparingTo(new BigDecimal("27.0"));
    }

    // Helper method to create ActivityMetrics for testing
    private ActivityMetrics createActivityMetric(
            Instant startedAt,
            double distance,
            int duration,
            int avgPace,
            int avgHR,
            int avgCadence
    ) {
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setActivityId(UUID.randomUUID());
        metrics.setUserId(userId);
        metrics.setStartedAt(startedAt);
        metrics.setTotalDistance(new BigDecimal(distance));
        metrics.setTotalDuration(duration);
        metrics.setAveragePace(avgPace);
        metrics.setAverageHeartRate(avgHR);
        metrics.setAverageCadence(avgCadence);
        metrics.setTotalAscent(50);
        metrics.setTotalDescent(50);
        metrics.setTotalCalories(600);
        metrics.setCalculatedAt(Instant.now());

        return metrics;
    }
}