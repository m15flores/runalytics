package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Builder
public record ActivityMetricsDto(
    @JsonProperty("activityId")
    UUID activityId,

    @JsonProperty("userId")
    String userId,

    @JsonProperty("startedAt")
    Instant startedAt,

    @JsonProperty("totalDistance")
    BigDecimal totalDistance,

    @JsonProperty("totalDuration")
    Integer totalDuration,

    @JsonProperty("totalCalories")
    Integer totalCalories,

    @JsonProperty("averagePace")
    Integer averagePace,

    @JsonProperty("averageHeartRate")
    Integer averageHeartRate,

    @JsonProperty("averageCadence")
    Integer averageCadence,

    @JsonProperty("hrZones")
    Map<String, Integer> hrZones,

    @JsonProperty("totalAscent")
    Integer totalAscent,

    @JsonProperty("totalDescent")
    Integer totalDescent,

    @JsonProperty("trainingEffect")
    Double trainingEffect,

    @JsonProperty("anaerobicTrainingEffect")
    Double anaerobicTrainingEffect
){}
