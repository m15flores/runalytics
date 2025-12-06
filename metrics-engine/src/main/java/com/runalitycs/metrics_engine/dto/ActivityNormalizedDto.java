package com.runalitycs.metrics_engine.dto;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ActivityNormalizedDto(
        @JsonProperty("activityId") UUID activityId,
        @JsonProperty("userId") String userId,
        @JsonProperty("device") String device,
        @JsonProperty("startedAt") Instant startedAt,

        // Session-level data (from FIT SESSION message)
        @JsonProperty("session") SessionData session,

        // Lap-level data (from FIT LAP messages)
        @JsonProperty("laps") List<LapData> laps,

        // Record-level samples (from FIT RECORD messages)
        @JsonProperty("samples") List<SampleData> samples,

        @JsonProperty("normalizedAt") Instant normalizedAt
) {
    public record SessionData(
            BigDecimal totalDistance,           // meters
            Integer totalTimerTime,             // seconds
            Integer totalElapsedTime,           // seconds
            Integer totalCalories,              // kcal

            Integer avgHeartRate,               // bpm
            Integer maxHeartRate,               // bpm
            Integer avgCadence,                 // rpm/spm
            Integer maxCadence,                 // rpm/spm

            BigDecimal enhancedAvgSpeed,        // m/s
            BigDecimal enhancedMaxSpeed,        // m/s

            Integer avgPower,                   // watts
            Integer maxPower,                   // watts
            Integer normalizedPower,            // watts

            Double avgVerticalOscillation,      // mm
            Double avgStanceTime,               // ms
            Double avgVerticalRatio,            // percent
            Integer avgStepLength,              // mm

            Integer totalAscent,                // meters
            Integer totalDescent,               // meters

            Double totalTrainingEffect,
            Double totalAnaerobicTrainingEffect,
            Double trainingLoadPeak,

            Integer workoutFeel,                // 0-100
            Integer workoutRpe,                 // 0-100

            // Time in HR zones (from FIT)
            Map<String, Integer> timeInHrZones, // {"Z1": 0, "Z2": 2580, "Z3": 2010, "Z4": 0, "Z5": 0}

            // Time in Power zones (from FIT)
            Map<String, Integer> timeInPowerZones,

            // Zone boundaries (from FIT)
            Integer maxHeartRateConfig,        // from user profile
            Integer restingHeartRate,
            Integer thresholdHeartRate,
            Integer functionalThresholdPower
    ) {}

    public record LapData(
            Integer lapNumber,
            Instant startTime,

            BigDecimal totalDistance,          // meters
            Integer totalTimerTime,            // seconds
            Integer totalElapsedTime,          // seconds
            Integer totalCalories,             // kcal

            Integer avgHeartRate,              // bpm
            Integer maxHeartRate,              // bpm
            Integer avgCadence,                // rpm/spm
            Integer maxCadence,                // rpm/spm

            BigDecimal enhancedAvgSpeed,       // m/s
            BigDecimal enhancedMaxSpeed,       // m/s

            Integer avgPower,                  // watts
            Integer maxPower,                  // watts
            Integer normalizedPower,           // watts

            Double avgVerticalOscillation,     // mm
            Double avgStanceTime,              // ms
            Double avgVerticalRatio,           // percent
            Integer avgStepLength,             // mm

            Integer totalAscent,               // meters
            Integer totalDescent,              // meters

            String intensity,                  // "warmup", "active", "cooldown", "rest"
            Integer wktStepIndex               // workout step index
    ) {}

    public record SampleData(
            Instant timestamp,
            Double latitude,
            Double longitude,
            Integer heartRate,                 // bpm
            Integer cadence,                   // rpm/spm
            Double altitude,                   // meters
            Double speed,                      // m/s
            Integer power,                     // watts
            Double distance                    // meters (accumulated)
    ) {}
}