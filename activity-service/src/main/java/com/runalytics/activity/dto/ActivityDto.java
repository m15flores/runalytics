package com.runalytics.activity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * DTO para recibir actividades desde fuentes externas (Garmin mock).
 * Usa Java Record para inmutabilidad y concisión.
 */
public record ActivityDto(
        @NotBlank(message = "userId cannot be blank")
        @JsonProperty("userId")
        String userId,

        @JsonProperty("device")
        String device,

        @NotNull(message = "timestamp cannot be null")
        @JsonProperty("timestamp")
        Instant timestamp,

        @JsonProperty("source")
        String source,

        @NotEmpty(message = "raw data cannot be empty")
        @JsonProperty("raw")
        Map<String, Object> raw
) {}