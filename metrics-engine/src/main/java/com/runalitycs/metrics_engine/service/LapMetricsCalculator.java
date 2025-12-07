package com.runalitycs.metrics_engine.service;

import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.dto.LapMetricsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class LapMetricsCalculator {

    /**
     * Calcula el pace de un lap en segundos por kilómetro.
     *
     * @param durationSeconds duración del lap en segundos
     * @param distanceMeters distancia del lap en metros
     * @return pace en segundos/km
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
     * Calcula el Grade Adjusted Pace para un lap.
     *
     * @param pace pace del lap en segundos/km
     * @param totalAscent elevación ganada en el lap (metros)
     * @param distance distancia del lap en metros
     * @return GAP en segundos/km
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
     * Genera un nombre descriptivo para el lap basándose en su intensidad.
     *
     * @param lapNumber número del lap
     * @param intensity intensidad ("warmup", "active", "cooldown", "rest")
     * @return nombre descriptivo
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
     * Calcula todas las métricas de un lap.
     *
     * @param lapData datos del lap del FIT
     * @return métricas calculadas del lap
     */
    public LapMetricsDto calculate(ActivityNormalizedDto.LapData lapData) {
        // Calcular pace y GAP
        Integer pace = calculateLapPace(lapData.totalTimerTime(), lapData.totalDistance());
        Integer gap = calculateLapGAP(pace, lapData.totalAscent(), lapData.totalDistance());

        // Generar nombre descriptivo
        String lapName = generateLapName(lapData.lapNumber(), lapData.intensity());

        // Construir DTO
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
                null,  // maxPace (no disponible en lap level)
                lapData.enhancedAvgSpeed(),
                lapData.enhancedMaxSpeed(),
                gap,

                // Heart Rate
                lapData.avgHeartRate(),
                lapData.maxHeartRate(),
                null,  // minHeartRate (no disponible en lap level)

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