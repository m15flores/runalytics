package com.runalitycs.metrics_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO que se publica en activities.metrics.calculated
 * Contiene métricas CALCULADAS + extraídas del FIT
 */
public record ActivityMetricsDto(
        @JsonProperty("activityId") UUID activityId,
        @JsonProperty("userId") String userId,

        // Basic metrics
        @JsonProperty("totalDistance") BigDecimal totalDistance,        // meters
        @JsonProperty("totalDuration") Integer totalDuration,           // seconds
        @JsonProperty("totalElapsedTime") Integer totalElapsedTime,     // seconds
        @JsonProperty("totalCalories") Integer totalCalories,           // kcal

        // Pace & Speed (CALCULATED)
        @JsonProperty("averagePace") Integer averagePace,               // sec/km
        @JsonProperty("maxPace") Integer maxPace,                       // sec/km
        @JsonProperty("averageSpeed") BigDecimal averageSpeed,          // m/s
        @JsonProperty("maxSpeed") BigDecimal maxSpeed,                  // m/s
        @JsonProperty("averageGAP") Integer averageGAP,                 // sec/km (Grade Adjusted Pace)

        // Heart Rate
        @JsonProperty("averageHeartRate") Integer averageHeartRate,     // bpm
        @JsonProperty("maxHeartRate") Integer maxHeartRate,             // bpm
        @JsonProperty("minHeartRate") Integer minHeartRate,             // bpm (from records)
        @JsonProperty("hrZones") Map<String, Integer> hrZones,          // time in seconds
        @JsonProperty("hrZonesPercentage") Map<String, Integer> hrZonesPercentage,  // percentage

        // Cadence
        @JsonProperty("averageCadence") Integer averageCadence,         // spm
        @JsonProperty("maxCadence") Integer maxCadence,                 // spm

        // Running Dynamics
        @JsonProperty("averageVerticalOscillation") Double averageVerticalOscillation,  // mm
        @JsonProperty("averageStanceTime") Double averageStanceTime,                    // ms
        @JsonProperty("averageVerticalRatio") Double averageVerticalRatio,              // percent
        @JsonProperty("averageStepLength") Integer averageStepLength,                   // mm

        // Power
        @JsonProperty("averagePower") Integer averagePower,             // watts
        @JsonProperty("maxPower") Integer maxPower,                     // watts
        @JsonProperty("normalizedPower") Integer normalizedPower,       // watts
        @JsonProperty("powerZones") Map<String, Integer> powerZones,    // time in seconds

        // Elevation
        @JsonProperty("totalAscent") Integer totalAscent,               // meters
        @JsonProperty("totalDescent") Integer totalDescent,             // meters

        // Training Load
        @JsonProperty("trainingEffect") Double trainingEffect,
        @JsonProperty("anaerobicTrainingEffect") Double anaerobicTrainingEffect,
        @JsonProperty("trainingLoadPeak") Double trainingLoadPeak,

        // Subjective
        @JsonProperty("workoutFeel") Integer workoutFeel,               // 0-100
        @JsonProperty("workoutRpe") Integer workoutRpe,                 // 0-100

        // Laps
        @JsonProperty("laps") List<LapMetricsDto> laps,

        // Metadata
        @JsonProperty("calculatedAt") Instant calculatedAt
) {}