package com.runalytics.normalizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO published to Kafka topic activities.normalized.
 * Schema aligned with metrics-engine's ActivityNormalizedDto.
 */
public record ActivityNormalizedDto(
        @NotNull(message = "activityId cannot be null")
        @JsonProperty("activityId")
        UUID activityId,

        @NotNull(message = "userId cannot be null")
        @JsonProperty("userId")
        String userId,

        @JsonProperty("device")
        String device,

        @NotNull(message = "startedAt cannot be null")
        @JsonProperty("startedAt")
        Instant startedAt,

        @JsonProperty("session")
        SessionData session,

        @JsonProperty("laps")
        List<LapData> laps,

        @JsonProperty("samples")
        List<SampleData> samples,

        @NotNull(message = "normalizedAt cannot be null")
        @JsonProperty("normalizedAt")
        Instant normalizedAt
) {

    public record SessionData(
            @JsonProperty("totalDistance") BigDecimal totalDistance,
            @JsonProperty("totalTimerTime") Integer totalTimerTime,
            @JsonProperty("totalElapsedTime") Integer totalElapsedTime,
            @JsonProperty("totalCalories") Integer totalCalories,

            @JsonProperty("avgHeartRate") Integer avgHeartRate,
            @JsonProperty("maxHeartRate") Integer maxHeartRate,
            @JsonProperty("avgCadence") Integer avgCadence,
            @JsonProperty("maxCadence") Integer maxCadence,

            @JsonProperty("enhancedAvgSpeed") BigDecimal enhancedAvgSpeed,
            @JsonProperty("enhancedMaxSpeed") BigDecimal enhancedMaxSpeed,

            @JsonProperty("avgPower") Integer avgPower,
            @JsonProperty("maxPower") Integer maxPower,
            @JsonProperty("normalizedPower") Integer normalizedPower,

            @JsonProperty("avgVerticalOscillation") Double avgVerticalOscillation,
            @JsonProperty("avgStanceTime") Double avgStanceTime,
            @JsonProperty("avgVerticalRatio") Double avgVerticalRatio,
            @JsonProperty("avgStepLength") Integer avgStepLength,

            @JsonProperty("totalAscent") Integer totalAscent,
            @JsonProperty("totalDescent") Integer totalDescent,

            @JsonProperty("totalTrainingEffect") Double totalTrainingEffect,
            @JsonProperty("totalAnaerobicTrainingEffect") Double totalAnaerobicTrainingEffect,
            @JsonProperty("trainingLoadPeak") Double trainingLoadPeak,

            @JsonProperty("workoutFeel") Integer workoutFeel,
            @JsonProperty("workoutRpe") Integer workoutRpe,

            @JsonProperty("timeInHrZones") Map<String, Integer> timeInHrZones,
            @JsonProperty("timeInPowerZones") Map<String, Integer> timeInPowerZones,

            @JsonProperty("maxHeartRateConfig") Integer maxHeartRateConfig,
            @JsonProperty("restingHeartRate") Integer restingHeartRate,
            @JsonProperty("thresholdHeartRate") Integer thresholdHeartRate,
            @JsonProperty("functionalThresholdPower") Integer functionalThresholdPower
    ) {}

    public record LapData(
            @JsonProperty("lapNumber") Integer lapNumber,
            @JsonProperty("startTime") Instant startTime,

            @JsonProperty("totalDistance") BigDecimal totalDistance,
            @JsonProperty("totalTimerTime") Integer totalTimerTime,
            @JsonProperty("totalElapsedTime") Integer totalElapsedTime,
            @JsonProperty("totalCalories") Integer totalCalories,

            @JsonProperty("avgHeartRate") Integer avgHeartRate,
            @JsonProperty("maxHeartRate") Integer maxHeartRate,
            @JsonProperty("avgCadence") Integer avgCadence,
            @JsonProperty("maxCadence") Integer maxCadence,

            @JsonProperty("enhancedAvgSpeed") BigDecimal enhancedAvgSpeed,
            @JsonProperty("enhancedMaxSpeed") BigDecimal enhancedMaxSpeed,

            @JsonProperty("avgPower") Integer avgPower,
            @JsonProperty("maxPower") Integer maxPower,
            @JsonProperty("normalizedPower") Integer normalizedPower,

            @JsonProperty("avgVerticalOscillation") Double avgVerticalOscillation,
            @JsonProperty("avgStanceTime") Double avgStanceTime,
            @JsonProperty("avgVerticalRatio") Double avgVerticalRatio,
            @JsonProperty("avgStepLength") Integer avgStepLength,

            @JsonProperty("totalAscent") Integer totalAscent,
            @JsonProperty("totalDescent") Integer totalDescent,

            @JsonProperty("intensity") String intensity,
            @JsonProperty("wktStepIndex") Integer wktStepIndex
    ) {}

    public record SampleData(
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("latitude") Double latitude,
            @JsonProperty("longitude") Double longitude,
            @JsonProperty("heartRate") Integer heartRate,
            @JsonProperty("cadence") Integer cadence,
            @JsonProperty("altitude") Double altitude,
            @JsonProperty("speed") Double speed,
            @JsonProperty("power") Integer power,
            @JsonProperty("distance") Double distance
    ) {}
}