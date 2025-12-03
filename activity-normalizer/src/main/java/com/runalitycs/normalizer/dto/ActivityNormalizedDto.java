package com.runalitycs.normalizer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO normalizado que se publica a Kafka topic activities.normalized.
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

        @JsonProperty("durationSeconds")
        Integer durationSeconds,

        @JsonProperty("distanceMeters")
        BigDecimal distanceMeters,

        @JsonProperty("samples")
        List<ActivitySample> samples,

        @NotNull(message = "normalizedAt cannot be null")
        @JsonProperty("normalizedAt")
        Instant normalizedAt
) {}