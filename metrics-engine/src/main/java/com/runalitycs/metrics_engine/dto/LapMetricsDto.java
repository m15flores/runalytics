package com.runalitycs.metrics_engine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record LapMetricsDto(
        @JsonProperty("lapNumber") Integer lapNumber,
        @JsonProperty("lapName") String lapName,
        @JsonProperty("intensity") String intensity,           // "warmup", "active", "cooldown", "rest"

        @JsonProperty("startTime") Instant startTime,
        @JsonProperty("distance") BigDecimal distance,         // meters
        @JsonProperty("duration") Integer duration,            // seconds
        @JsonProperty("calories") Integer calories,            // kcal

        // Pace & Speed (CALCULATED)
        @JsonProperty("averagePace") Integer averagePace,      // sec/km
        @JsonProperty("maxPace") Integer maxPace,              // sec/km
        @JsonProperty("averageSpeed") BigDecimal averageSpeed, // m/s
        @JsonProperty("maxSpeed") BigDecimal maxSpeed,         // m/s
        @JsonProperty("averageGAP") Integer averageGAP,        // sec/km

        // Heart Rate
        @JsonProperty("averageHeartRate") Integer averageHeartRate,  // bpm
        @JsonProperty("maxHeartRate") Integer maxHeartRate,          // bpm
        @JsonProperty("minHeartRate") Integer minHeartRate,          // bpm

        // Cadence
        @JsonProperty("averageCadence") Integer averageCadence,      // spm
        @JsonProperty("maxCadence") Integer maxCadence,              // spm

        // Running Dynamics
        @JsonProperty("averageVerticalOscillation") Double averageVerticalOscillation,  // mm
        @JsonProperty("averageStanceTime") Double averageStanceTime,                    // ms
        @JsonProperty("averageVerticalRatio") Double averageVerticalRatio,              // percent
        @JsonProperty("averageStepLength") Integer averageStepLength,                   // mm

        // Power
        @JsonProperty("averagePower") Integer averagePower,          // watts
        @JsonProperty("maxPower") Integer maxPower,                  // watts
        @JsonProperty("normalizedPower") Integer normalizedPower,    // watts

        // Elevation
        @JsonProperty("totalAscent") Integer totalAscent,            // meters
        @JsonProperty("totalDescent") Integer totalDescent           // meters
) {}