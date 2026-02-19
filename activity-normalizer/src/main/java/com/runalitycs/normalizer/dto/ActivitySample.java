package com.runalitycs.normalizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Represents a single time-series data point within an activity.
 * All fields except timestamp are optional.
 */
public record ActivitySample (

        @NotNull(message = "timestamp cannot be null")
        @JsonProperty("timestamp")
        Instant timestamp,

        @JsonProperty("latitude")
        Double latitude,

        @JsonProperty("longitude")
        Double longitude,

        @JsonProperty("heartRate")
        Integer heartRate,

        @JsonProperty("paceSecondsPerKm")
        Integer paceSecondsPerKm,

        @JsonProperty("altitude")
        Double altitude,

        @JsonProperty("cadence")
        Integer cadence
){}
