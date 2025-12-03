package com.runalitycs.normalizer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Datos parseados de un archivo FIT.
 */
public record ParsedFitData(
        Instant startedAt,
        Integer durationSeconds,
        BigDecimal distanceMeters,
        List<ActivitySample> samples
) {}