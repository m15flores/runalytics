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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.withinPercentage;
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

    @Test
    void shouldCalculateWeightedAveragePace() {
        // Given - 3 activities with different paces and distances
        // Activity 1: 10km @ 6:00 min/km (360 sec/km)
        // Activity 2: 5km @ 5:00 min/km (300 sec/km)
        // Activity 3: 5km @ 4:00 min/km (240 sec/km)
        // Expected weighted avg: (10*360 + 5*300 + 5*240) / 20 = (3600 + 1500 + 1200) / 20 = 315 sec/km

        List<ActivityMetrics> activities = Arrays.asList(
                createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170),
                createActivityMetric(baseTime.minusSeconds(86400), 5.0, 1500, 300, 150, 172),
                createActivityMetric(baseTime.minusSeconds(172800), 5.0, 1200, 240, 140, 168)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        WeeklyStats weekStats = stats.get(0);
        assertThat(weekStats.getAveragePace()).isEqualTo(315); // Weighted average
    }

    @Test
    void shouldCalculateWeightedAverageHeartRate() {
        // Given - 3 activities with different HR and durations
        // Activity 1: 3600s @ 145 bpm
        // Activity 2: 1800s @ 150 bpm
        // Activity 3: 1800s @ 140 bpm
        // Expected weighted avg: (3600*145 + 1800*150 + 1800*140) / 7200 = 145 bpm

        List<ActivityMetrics> activities = Arrays.asList(
                createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170),
                createActivityMetric(baseTime.minusSeconds(86400), 5.0, 1800, 300, 150, 172),
                createActivityMetric(baseTime.minusSeconds(172800), 5.0, 1800, 240, 140, 168)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        WeeklyStats weekStats = stats.get(0);
        assertThat(weekStats.getAverageHeartRate()).isEqualTo(145);
    }

    @Test
    void shouldCalculateWeightedAverageCadence() {
        // Given - 3 activities with different cadence and durations
        // Activity 1: 3600s @ 170 spm
        // Activity 2: 1800s @ 172 spm
        // Activity 3: 1800s @ 168 spm
        // Expected weighted avg: (3600*170 + 1800*172 + 1800*168) / 7200 = 170 spm

        List<ActivityMetrics> activities = Arrays.asList(
                createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170),
                createActivityMetric(baseTime.minusSeconds(86400), 5.0, 1800, 300, 150, 172),
                createActivityMetric(baseTime.minusSeconds(172800), 5.0, 1800, 240, 140, 168)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        WeeklyStats weekStats = stats.get(0);
        assertThat(weekStats.getAverageCadence()).isEqualTo(170);
    }

    @Test
    void shouldAggregateHrZonesDistribution() {
        // Given - Activities with HR zones
        ActivityMetrics activity1 = createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170);
        activity1.setHrZones(Map.of("Z1", 600, "Z2", 2400, "Z3", 600));

        ActivityMetrics activity2 = createActivityMetric(baseTime.minusSeconds(86400), 5.0, 1800, 300, 150, 172);
        activity2.setHrZones(Map.of("Z2", 1200, "Z3", 600));

        List<ActivityMetrics> activities = Arrays.asList(activity1, activity2);

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        WeeklyStats weekStats = stats.get(0);
        Map<String, Integer> hrZones = weekStats.getHrZonesDistribution();

        assertThat(hrZones).isNotNull();
        assertThat(hrZones.get("Z1")).isEqualTo(600);
        assertThat(hrZones.get("Z2")).isEqualTo(3600);
        assertThat(hrZones.get("Z3")).isEqualTo(1200);
    }

    @Test
    void shouldCalculateTrendsAcrossWeeks() {
        // Given - 3 weeks with increasing distance
        // Week 49: 50km
        // Week 48: 48km (+4.2% vs previous)
        // Week 47: 46km (+4.3% vs previous)

        Instant week1 = baseTime; // Week 49
        Instant week2 = baseTime.minusSeconds(7 * 86400); // Week 48
        Instant week3 = week2.minusSeconds(7 * 86400); // Week 47

        List<ActivityMetrics> activities = Arrays.asList(
                // Week 49: 50km
                createActivityMetric(week1, 25.0, 7200, 360, 145, 170),
                createActivityMetric(week1.minusSeconds(86400), 25.0, 7200, 360, 145, 170),

                // Week 48: 48km
                createActivityMetric(week2, 24.0, 6912, 360, 145, 170),
                createActivityMetric(week2.minusSeconds(86400), 24.0, 6912, 360, 145, 170),

                // Week 47: 46km
                createActivityMetric(week3, 23.0, 6624, 360, 145, 170),
                createActivityMetric(week3.minusSeconds(86400), 23.0, 6624, 360, 145, 170)
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
        assertThat(week49.getDistanceChangePercent()).isCloseTo(4.17, withinPercentage(5)); // (50-48)/48 * 100
        assertThat(week49.getTrend()).isEqualTo("improving");

        // Week 48
        WeeklyStats week48 = stats.get(1);
        assertThat(week48.getDistanceChangePercent()).isCloseTo(4.35, withinPercentage(5)); // (48-46)/46 * 100
        assertThat(week48.getTrend()).isEqualTo("improving");
    }

    @Test
    void shouldHandleWeekWithNoActivities() {
        // Given - Only activities in week 49, nothing in weeks 48 and 47
        List<ActivityMetrics> activities = Arrays.asList(
                createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170),
                createActivityMetric(baseTime.minusSeconds(86400), 12.0, 4320, 360, 150, 172)
        );

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 3);

        // Then
        assertThat(stats).hasSize(3);

        // Week 49 - has activities
        WeeklyStats week49 = stats.get(0);
        assertThat(week49.getTotalActivities()).isEqualTo(2);
        assertThat(week49.getTotalDistance()).isEqualByComparingTo(new BigDecimal("22.0"));

        // Week 48 - empty
        WeeklyStats week48 = stats.get(1);
        assertThat(week48.getTotalActivities()).isEqualTo(0);
        assertThat(week48.getTotalDistance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(week48.getAveragePace()).isNull();
        assertThat(week48.getAverageHeartRate()).isNull();

        // Week 47 - empty
        WeeklyStats week47 = stats.get(2);
        assertThat(week47.getTotalActivities()).isEqualTo(0);
        assertThat(week47.getTotalDistance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldHandleActivitiesWithMissingData() {
        // Given - Activities with some null fields
        ActivityMetrics activity1 = createActivityMetric(baseTime, 10.0, 3600, 360, 145, 170);

        ActivityMetrics activity2 = createActivityMetric(baseTime.minusSeconds(86400), 5.0, 1800, 300, 150, 172);
        activity2.setAveragePace(null); // Missing pace

        ActivityMetrics activity3 = createActivityMetric(baseTime.minusSeconds(172800), 8.0, 2880, 240, 140, 168);
        activity3.setAverageHeartRate(null); // Missing HR
        activity3.setAverageCadence(null); // Missing cadence

        List<ActivityMetrics> activities = Arrays.asList(activity1, activity2, activity3);

        when(activityMetricsRepository.findByUserIdAndDateRange(
                eq(userId), any(Instant.class), any(Instant.class)
        )).thenReturn(activities);

        // When
        List<WeeklyStats> stats = service.getWeeklyStats(userId, 1);

        // Then
        WeeklyStats weekStats = stats.get(0);

        // Should still aggregate
        assertThat(weekStats.getTotalActivities()).isEqualTo(3);
        assertThat(weekStats.getTotalDistance()).isEqualByComparingTo(new BigDecimal("23.0"));
        assertThat(weekStats.getTotalDuration()).isEqualTo(8280);

        // Weighted averages should exclude null values
        // Pace: only activity1 and activity3 have pace
        // Expected: (10*360 + 8*240) / 18 = (3600 + 1920) / 18 = 306.67 ≈ 307
        assertThat(weekStats.getAveragePace()).isEqualTo(307);

        // HR: only activity1 and activity2 have HR
        // Expected: (3600*145 + 1800*150) / 5400 = 146.67 ≈ 147
        assertThat(weekStats.getAverageHeartRate()).isEqualTo(147);

        // Cadence: only activity1 and activity2 have cadence
        // Expected: (3600*170 + 1800*172) / 5400 = 170.67 ≈ 171
        assertThat(weekStats.getAverageCadence()).isEqualTo(171);
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