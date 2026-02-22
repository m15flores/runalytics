package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityMetricsCalculator {

    private final LapMetricsCalculator lapMetricsCalculator;
    private final Clock clock;

    public Integer calculatePace(Integer durationSeconds, BigDecimal distanceMeters) {
        return lapMetricsCalculator.calculateLapPace(durationSeconds, distanceMeters);
    }

    public Map<String, Integer> calculateHrZonesPercentage(
            Map<String, Integer> hrZones,
            Integer totalDuration) {

        if (hrZones == null || hrZones.isEmpty() || totalDuration == null || totalDuration == 0) {
            return Map.of();
        }

        Map<String, Integer> hrZonesPercentage = new HashMap<>();
        hrZones.forEach((zone, time) -> hrZonesPercentage.put(zone, (time * 100) / totalDuration));
        return hrZonesPercentage;
    }

    public Integer calculateMinHeartRate(List<ActivityNormalizedDto.SampleData> samples) {
        if (samples == null || samples.isEmpty()) return null;

        return samples.stream()
                .map(ActivityNormalizedDto.SampleData::heartRate)
                .filter(hr -> hr != null)
                .min(Integer::compareTo)
                .orElse(null);
    }

    /**
     * Calculates Grade Adjusted Pace (GAP).
     * Formula: GAP = pace / (1 + (totalAscent / totalDistance) * factor)
     * Factor = 10.0: each 100m of positive gradient equals ~10% more effort.
     */
    public Integer calculateGAP(Integer pace, Integer totalAscent, BigDecimal totalDistance) {
        return lapMetricsCalculator.calculateLapGAP(pace, totalAscent, totalDistance);
    }

    /**
     * Returns the fastest lap pace (lowest sec/km value).
     */
    public Integer calculateMaxPaceFromLaps(List<LapMetricsDto> laps) {
        if (laps == null || laps.isEmpty()) return null;

        return laps.stream()
                .map(LapMetricsDto::averagePace)
                .filter(pace -> pace != null && pace > 0)
                .min(Integer::compareTo)
                .orElse(null);
    }

    public ActivityMetricsDto calculate(ActivityNormalizedDto input) {
        var session = input.session();

        Integer averagePace = calculatePace(session.totalTimerTime(), session.totalDistance());
        Integer averageGAP = calculateGAP(averagePace, session.totalAscent(), session.totalDistance());
        Integer minHeartRate = calculateMinHeartRate(input.samples());
        Map<String, Integer> hrZonesPercentage = calculateHrZonesPercentage(
                session.timeInHrZones(), session.totalTimerTime());

        List<LapMetricsDto> lapMetrics = input.laps().stream()
                .map(lapMetricsCalculator::calculate)
                .toList();

        Integer maxPace = calculateMaxPaceFromLaps(lapMetrics);

        Map<String, Integer> hrZones = session.timeInHrZones() != null
                ? session.timeInHrZones()
                : Map.of();

        return new ActivityMetricsDto(
                input.activityId(),
                input.userId(),
                session.totalDistance(),
                session.totalTimerTime(),
                session.totalElapsedTime(),
                session.totalCalories(),
                averagePace,
                maxPace,
                session.enhancedAvgSpeed(),
                session.enhancedMaxSpeed(),
                averageGAP,
                session.avgHeartRate(),
                session.maxHeartRate(),
                minHeartRate,
                hrZones,
                hrZonesPercentage,
                session.avgCadence(),
                session.maxCadence(),
                session.avgVerticalOscillation(),
                session.avgStanceTime(),
                session.avgVerticalRatio(),
                session.avgStepLength(),
                session.avgPower(),
                session.maxPower(),
                session.normalizedPower(),
                session.timeInPowerZones(),
                session.totalAscent(),
                session.totalDescent(),
                session.totalTrainingEffect(),
                session.totalAnaerobicTrainingEffect(),
                session.trainingLoadPeak(),
                session.workoutFeel(),
                session.workoutRpe(),
                lapMetrics,
                Instant.now(clock)
        );
    }
}