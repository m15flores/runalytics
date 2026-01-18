package com.runalitycs.report_generator.service;


import com.runalitycs.report_generator.entity.ActivityMetrics;
import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.repository.ActivityMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyAggregationService {

    private final ActivityMetricsRepository activityMetricsRepository;
    private final Clock clock;

    private static final String DEFAULT_TIMEZONE = "Europe/Paris";

    public List<WeeklyStats> getWeeklyStats(String userId, int numberOfWeeks) {
        log.info("Calculating weekly stats for userId: {}, weeks: {}", userId, numberOfWeeks);

        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        LocalDate now = LocalDate.now(clock);
        LocalDate startDate = now.minusWeeks(numberOfWeeks);

        Instant startInstant = startDate.atStartOfDay(zoneId).toInstant();
        Instant endInstant = now.plusDays(1).atStartOfDay(zoneId).toInstant();

        List<ActivityMetrics> activities = activityMetricsRepository.findByUserIdAndDateRange(
                userId, startInstant, endInstant
        );

        log.debug("Found {} activities for aggregation", activities.size());

        // Group activities by week
        Map<String, List<ActivityMetrics>> activitiesByWeek = activities.stream()
                .collect(Collectors.groupingBy(activity -> getWeekKey(activity.getStartedAt(), zoneId)));

        // Calculate stats for each week
        List<WeeklyStats> weeklyStatsList = new ArrayList<>();
        for (int i = 0; i < numberOfWeeks; i++) {
            LocalDate weekDate = now.minusWeeks(i);
            String weekKey = getWeekKey(weekDate.atStartOfDay(zoneId).toInstant(), zoneId);

            List<ActivityMetrics> weekActivities = activitiesByWeek.getOrDefault(weekKey, Collections.emptyList());
            WeeklyStats stats = calculateWeekStats(weekActivities, weekDate, zoneId);
            weeklyStatsList.add(stats);
        }

        return weeklyStatsList;
    }

    private WeeklyStats calculateWeekStats(
            List<ActivityMetrics> activities,
            LocalDate weekDate,
            ZoneId zoneId
    ) {
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
        int weekNumber = weekDate.get(weekFields.weekOfWeekBasedYear());
        int year = weekDate.get(weekFields.weekBasedYear());

        if (activities.isEmpty()) {
            return WeeklyStats.builder()
                    .weekNumber(weekNumber)
                    .year(year)
                    .totalActivities(0)
                    .totalDistance(BigDecimal.ZERO)
                    .totalDuration(0)
                    .totalCalories(0)
                    .build();
        }

        // Basic aggregations
        int totalActivities = activities.size();

        BigDecimal totalDistance = activities.stream()
                .map(ActivityMetrics::getTotalDistance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalDuration = activities.stream()
                .map(ActivityMetrics::getTotalDuration)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        int totalCalories = activities.stream()
                .map(ActivityMetrics::getTotalCalories)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();

        return WeeklyStats.builder()
                .weekNumber(weekNumber)
                .year(year)
                .totalActivities(totalActivities)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalCalories(totalCalories)
                .build();
    }

    private String getWeekKey(Instant instant, ZoneId zoneId) {
        LocalDate date = instant.atZone(zoneId).toLocalDate();
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return year + "-W" + String.format("%02d", weekNumber);
    }
}