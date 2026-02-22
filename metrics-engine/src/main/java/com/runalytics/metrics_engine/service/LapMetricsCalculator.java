package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.dto.LapMetricsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LapMetricsCalculator {

    /**
     * Calculates lap pace in seconds per kilometre.
     *
     * @param durationSeconds lap duration in seconds
     * @param distanceMeters  lap distance in metres
     * @return pace in seconds/km
     */
    public Integer calculateLapPace(Integer durationSeconds, BigDecimal distanceMeters) {
        if (distanceMeters == null || distanceMeters.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        return BigDecimal.valueOf(durationSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(distanceMeters, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * Calculates Grade Adjusted Pace (GAP) for a lap.
     *
     * @param pace        lap pace in seconds/km
     * @param totalAscent elevation gain in the lap (metres)
     * @param distance    lap distance in metres
     * @return GAP in seconds/km
     */
    public Integer calculateLapGAP(Integer pace, Integer totalAscent, BigDecimal distance) {
        if (pace == null) {
            return null;
        }

        if (totalAscent == null || totalAscent == 0 ||
                distance == null || distance.compareTo(BigDecimal.ZERO) == 0) {
            return pace;
        }

        double elevationFactor = 10.0;
        double elevationRatio = totalAscent / distance.doubleValue();
        double adjustment = 1.0 + (elevationRatio * elevationFactor);

        return (int) Math.round(pace / adjustment);
    }

    /**
     * Generates a descriptive name for the lap based on its intensity.
     *
     * @param lapNumber lap number
     * @param intensity intensity label ("warmup", "active", "cooldown", "rest")
     * @return descriptive name
     */
    public String generateLapName(Integer lapNumber, String intensity) {
        if (intensity == null) {
            return "Lap " + lapNumber;
        }

        return switch (intensity.toLowerCase()) {
            case "warmup" -> "Warmup";
            case "cooldown" -> "Cooldown";
            case "rest" -> "Rest";
            case "active" -> "Interval " + lapNumber;
            default -> "Lap " + lapNumber;
        };
    }

    /**
     * Calculates all metrics for a lap.
     *
     * @param lapData lap data from the FIT file
     * @return calculated lap metrics
     */
    public LapMetricsDto calculate(ActivityNormalizedDto.LapData lapData) {
        Integer pace = calculateLapPace(lapData.totalTimerTime(), lapData.totalDistance());
        Integer gap = calculateLapGAP(pace, lapData.totalAscent(), lapData.totalDistance());

        String lapName = generateLapName(lapData.lapNumber(), lapData.intensity());

        return new LapMetricsDto(
                lapData.lapNumber(),
                lapName,
                lapData.intensity(),

                lapData.startTime(),
                lapData.totalDistance(),
                lapData.totalTimerTime(),
                lapData.totalCalories(),

                // Pace & Speed
                pace,
                null,  // maxPace (not available at lap level)
                lapData.enhancedAvgSpeed(),
                lapData.enhancedMaxSpeed(),
                gap,

                // Heart Rate
                lapData.avgHeartRate(),
                lapData.maxHeartRate(),
                null,  // minHeartRate (not available at lap level)

                // Cadence
                lapData.avgCadence(),
                lapData.maxCadence(),

                // Running Dynamics
                lapData.avgVerticalOscillation(),
                lapData.avgStanceTime(),
                lapData.avgVerticalRatio(),
                lapData.avgStepLength(),

                // Power
                lapData.avgPower(),
                lapData.maxPower(),
                lapData.normalizedPower(),

                // Elevation
                lapData.totalAscent(),
                lapData.totalDescent()
        );
    }
}