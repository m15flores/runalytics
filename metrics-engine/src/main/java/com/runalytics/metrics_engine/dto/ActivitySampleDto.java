package com.runalytics.metrics_engine.dto;

import java.time.Instant;

public record ActivitySampleDto(
        Instant timestamp,
        Double latitude,
        Double longitude,
        Integer heartRate,
        Integer cadence,
        Double altitude,
        Double speed,
        Integer power,
        Double distance
) {}
