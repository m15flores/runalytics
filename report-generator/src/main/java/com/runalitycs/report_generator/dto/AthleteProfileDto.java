package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.UUID;

public record AthleteProfileDto(
        @JsonProperty("id")
        UUID id,

        @NotBlank(message = "userId is required")
        @JsonProperty("userId")
        String userId,

        @NotBlank(message = "name is required")
        @JsonProperty("name")
        String name,

        @Min(value = 10, message = "age must be at least 10")
        @Max(value = 120, message = "age must be at most 120")
        @JsonProperty("age")
        Integer age,

        @Positive(message = "weight must be positive")
        @JsonProperty("weight")
        Double weight,

        @Min(value = 100, message = "maxHeartRate must be at least 100")
        @Max(value = 250, message = "maxHeartRate must be at most 250")
        @JsonProperty("maxHeartRate")
        Integer maxHeartRate,

        @JsonProperty("currentGoal")
        String currentGoal,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("updatedAt")
        Instant updatedAt
) {
}