package com.runalytics.normalizer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ParsedFitData(
        Instant startedAt,
        Integer durationSeconds,
        BigDecimal distanceMeters,
        SessionInfo session,
        List<LapInfo> laps,
        List<ActivitySample> samples
) {

    public record SessionInfo(
            BigDecimal totalDistance,
            Integer totalTimerTime,
            Integer totalElapsedTime,
            Integer totalCalories,

            Integer avgHeartRate,
            Integer maxHeartRate,
            Integer avgCadence,
            Integer maxCadence,

            BigDecimal enhancedAvgSpeed,
            BigDecimal enhancedMaxSpeed,

            Integer avgPower,
            Integer maxPower,
            Integer normalizedPower,

            Double avgVerticalOscillation,
            Double avgStanceTime,
            Double avgVerticalRatio,
            Integer avgStepLength,

            Integer totalAscent,
            Integer totalDescent,

            Double totalTrainingEffect,
            Double totalAnaerobicTrainingEffect,
            Double trainingLoadPeak,

            Integer workoutFeel,
            Integer workoutRpe,

            Map<String, Integer> timeInHrZones,
            Map<String, Integer> timeInPowerZones,

            Integer maxHeartRateConfig,
            Integer restingHeartRate,
            Integer thresholdHeartRate,
            Integer functionalThresholdPower
    ) {}

    public record LapInfo(
            Integer lapNumber,
            Instant startTime,

            BigDecimal totalDistance,
            Integer totalTimerTime,
            Integer totalElapsedTime,
            Integer totalCalories,

            Integer avgHeartRate,
            Integer maxHeartRate,
            Integer avgCadence,
            Integer maxCadence,

            BigDecimal enhancedAvgSpeed,
            BigDecimal enhancedMaxSpeed,

            Integer avgPower,
            Integer maxPower,
            Integer normalizedPower,

            Double avgVerticalOscillation,
            Double avgStanceTime,
            Double avgVerticalRatio,
            Integer avgStepLength,

            Integer totalAscent,
            Integer totalDescent,

            String intensity,
            Integer wktStepIndex
    ) {}
}