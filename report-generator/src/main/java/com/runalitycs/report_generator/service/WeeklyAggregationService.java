package com.runalitycs.report_generator.service;


import com.runalitycs.report_generator.entity.ActivityMetrics;
import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.repository.ActivityMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

        calculateTrends(weeklyStatsList);

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

        Integer averagePace = calculateWeightedAveragePace(activities);
        Integer averageHeartRate = calculateWeightedAverageHeartRate(activities);
        Integer averageCadence = calculateWeightedAverageCadence(activities);
        Map<String, Integer> hrZonesDistribution = aggregateHrZones(activities);


        return WeeklyStats.builder()
                .weekNumber(weekNumber)
                .year(year)
                .totalActivities(totalActivities)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
                .totalCalories(totalCalories)
                .averagePace(averagePace)
                .averageHeartRate(averageHeartRate)
                .averageCadence(averageCadence)
                .hrZonesDistribution(hrZonesDistribution)
                .build();
    }

    private String getWeekKey(Instant instant, ZoneId zoneId) {
        LocalDate date = instant.atZone(zoneId).toLocalDate();
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);
        int weekNumber = date.get(weekFields.weekOfWeekBasedYear());
        int year = date.get(weekFields.weekBasedYear());
        return year + "-W" + String.format("%02d", weekNumber);
    }

    private Integer calculateWeightedAveragePace(List<ActivityMetrics> activities) {
        double totalWeightedPace = 0.0;
        double totalDistance = 0.0;

        for (ActivityMetrics activity : activities) {
            if (activity.getAveragePace() != null &&
                    activity.getTotalDistance() != null &&
                    activity.getTotalDistance().compareTo(BigDecimal.ZERO) > 0) {

                double distance = activity.getTotalDistance().doubleValue();
                totalWeightedPace += activity.getAveragePace() * distance;
                totalDistance += distance;
            }
        }

        if (totalDistance == 0) {
            return null;
        }

        return (int) Math.round(totalWeightedPace / totalDistance);
    }

    private Integer calculateWeightedAverageHeartRate(List<ActivityMetrics> activities) {
        double totalWeightedHR = 0.0;
        int totalDuration = 0;

        for (ActivityMetrics activity : activities) {
            if (activity.getAverageHeartRate() != null &&
                    activity.getTotalDuration() != null &&
                    activity.getTotalDuration() > 0) {

                totalWeightedHR += activity.getAverageHeartRate() * activity.getTotalDuration();
                totalDuration += activity.getTotalDuration();
            }
        }

        if (totalDuration == 0) {
            return null;
        }

        return (int) Math.round(totalWeightedHR / totalDuration);
    }

    private Integer calculateWeightedAverageCadence(List<ActivityMetrics> activities) {
        double totalWeightedCadence = 0.0;
        int totalDuration = 0;

        for (ActivityMetrics activity : activities) {
            if (activity.getAverageCadence() != null &&
                    activity.getTotalDuration() != null &&
                    activity.getTotalDuration() > 0) {

                totalWeightedCadence += activity.getAverageCadence() * activity.getTotalDuration();
                totalDuration += activity.getTotalDuration();
            }
        }

        if (totalDuration == 0) {
            return null;
        }

        return (int) Math.round(totalWeightedCadence / totalDuration);
    }

    private Map<String, Integer> aggregateHrZones(List<ActivityMetrics> activities) {
        Map<String, Integer> aggregated = new HashMap<>();

        for (ActivityMetrics activity : activities) {
            if (activity.getHrZones() != null) {
                activity.getHrZones().forEach((zone, seconds) ->
                        aggregated.merge(zone, seconds, Integer::sum)
                );
            }
        }

        return aggregated.isEmpty() ? null : aggregated;
    }

    private void calculateTrends(List<WeeklyStats> weeklyStatsList) {
        for (int i = 0; i < weeklyStatsList.size() - 1; i++) {
            WeeklyStats currentWeek = weeklyStatsList.get(i);
            WeeklyStats previousWeek = weeklyStatsList.get(i + 1);

            BigDecimal currentDistance = currentWeek.getTotalDistance();
            BigDecimal previousDistance = previousWeek.getTotalDistance();

            if (previousDistance != null && previousDistance.compareTo(BigDecimal.ZERO) > 0) {
                // Calculate % change
                BigDecimal change = currentDistance.subtract(previousDistance);
                double changePercent = change.divide(previousDistance, 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"))
                        .doubleValue();

                currentWeek.setDistanceChangePercent(changePercent);

                // Determine trend
                if (changePercent > 2.0) {
                    currentWeek.setTrend("improving");
                } else if (changePercent < -2.0) {
                    currentWeek.setTrend("declining");
                } else {
                    currentWeek.setTrend("stable");
                }
            }
        }
    }
}