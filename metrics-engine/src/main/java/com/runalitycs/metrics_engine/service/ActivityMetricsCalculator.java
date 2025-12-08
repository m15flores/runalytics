package com.runalitycs.metrics_engine.service;

import com.runalitycs.metrics_engine.dto.ActivityMetricsDto;
import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.dto.LapMetricsDto;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ActivityMetricsCalculator {

    private final LapMetricsCalculator lapMetricsCalculator;

    public ActivityMetricsCalculator(LapMetricsCalculator lapMetricsCalculator) {
        this.lapMetricsCalculator = lapMetricsCalculator;
    }

    /**
     * Calcula el pace promedio en segundos por kilómetro.
     * Ejemplos:
     *      *   - 4777s / 13.138km = 363 seg/km (6:03 /km)
     *      *   - 3600s / 10km = 360 seg/km (6:00 /km)
     * @param durationSeconds duración total en segundos
     * @param distanceMeters distancia total en metros
     * @return pace en segundos/km, o null si la distancia es 0
     */
    public Integer calculatePace(Integer durationSeconds, BigDecimal distanceMeters) {
        if(distanceMeters == null || distanceMeters.compareTo(BigDecimal.ZERO) == 0) return null;

        BigDecimal pace = BigDecimal.valueOf(durationSeconds)
                .multiply(BigDecimal.valueOf(1000))
                .divide(distanceMeters, 0, RoundingMode.HALF_UP);

        return pace.intValue();
    }

    /**
     * Calcula el porcentaje de tiempo en cada zona de frecuencia cardíaca.
     *
     * @param hrZones mapa con tiempo en segundos por zona {"Z1": 0, "Z2": 2580, ...}
     * @param totalDuration duración total de la actividad en segundos
     * @return mapa con porcentajes por zona {"Z1": 0, "Z2": 56, ...}
     */
    public Map<String, Integer> calculateHrZonesPercentage(
            Map<String, Integer> hrZones,
            Integer totalDuration) {

        if (hrZones == null || hrZones.isEmpty() || totalDuration == null || totalDuration == 0) return Map.of();

        Map<String, Integer> hrZonesPercentage = new HashMap<>();
        hrZones.forEach((zone, time) -> {
            hrZonesPercentage.put(zone, (time * 100) / totalDuration);
        });

        return hrZonesPercentage;
    }

    /**
     * Calcula la frecuencia cardíaca mínima de todos los samples.
     *
     * @param samples lista de samples con datos de HR
     * @return HR mínimo en bpm, o null si no hay datos
     */
    public Integer calculateMinHeartRate(List<ActivityNormalizedDto.SampleData> samples) {

        if(samples == null || samples.isEmpty()) return null;

        return samples.stream()
                .map(sample -> sample.heartRate())
                .filter(hr -> hr != null)
                .min(Integer::compareTo)
                .orElse(null);
    }

    /**
     * Calcula el Grade Adjusted Pace (GAP).
     *
     * El GAP ajusta el pace según la elevación ganada, mostrando el "pace equivalente"
     * si hubieras corrido en llano.
     *
     * Fórmula: GAP = pace / (1 + (totalAscent / totalDistance) * factor)
     *
     * Factor típico: 10
     * Esto significa que cada 100m de desnivel positivo equivale a ~10% más esfuerzo.
     *
     * Ejemplo:
     * - Pace real: 6:04 /km (364 seg/km)
     * - Ascenso: 120m en 10km
     * - GAP: 5:25 /km (325 seg/km) ← Tu pace "real" ajustado
     *
     * @param pace pace promedio en segundos/km
     * @param totalAscent elevación ganada en metros
     * @param totalDistance distancia total en metros
     * @return GAP en segundos/km, o null si pace es null
     */
    public Integer calculateGAP(Integer pace, Integer totalAscent, BigDecimal totalDistance) {
        if (pace == null) {
            return null;
        }

        if (totalAscent == null || totalAscent == 0 ||
                totalDistance == null || totalDistance.compareTo(BigDecimal.ZERO) == 0) {
            return pace;  // Sin desnivel, GAP = pace normal
        }

        // Factor de ajuste: 10 (cada 100m de ascenso = 10% más esfuerzo)
        double elevationFactor = 10.0;

        // Calcular ratio de elevación: ascent / distance
        double elevationRatio = totalAscent / totalDistance.doubleValue();

        // Ajuste multiplicador: 1 + (ratio * factor)
        double adjustment = 1.0 + (elevationRatio * elevationFactor);

        // GAP = pace / adjustment
        return (int) Math.round(pace / adjustment);
    }

    /**
     * Calcula el pace máximo (más rápido) a partir de los laps.
     *
     * El pace máximo es el pace del lap más rápido.
     * Nota: pace más bajo = más rápido (ej: 300 seg/km es más rápido que 400 seg/km)
     *
     * @param laps lista de métricas de laps
     * @return pace máximo (mínimo numérico) en segundos/km, o null si no hay laps
     */
    public Integer calculateMaxPaceFromLaps(List<LapMetricsDto> laps) {
        if (laps == null || laps.isEmpty()) {
            return null;
        }

        return laps.stream()
                .map(LapMetricsDto::averagePace)
                .filter(pace -> pace != null && pace > 0)
                .min(Integer::compareTo)  // Mínimo porque pace más bajo = más rápido
                .orElse(null);
    }

    /**
     * Actualiza las métricas de actividad con los laps procesados.
     *
     * Calcula el max pace a partir de los laps y añade los laps al DTO.
     *
     * @param metrics métricas de actividad sin laps
     * @param laps lista de métricas de laps calculadas
     * @return nuevo DTO con laps y max pace actualizado
     */
    public ActivityMetricsDto addLapsToMetrics(ActivityMetricsDto metrics, List<LapMetricsDto> laps) {
        Integer maxPace = calculateMaxPaceFromLaps(laps);

        return new ActivityMetricsDto(
                metrics.activityId(),
                metrics.userId(),
                metrics.totalDistance(),
                metrics.totalDuration(),
                metrics.totalElapsedTime(),
                metrics.totalCalories(),
                metrics.averagePace(),
                maxPace,  // ← ACTUALIZADO con el pace del lap más rápido
                metrics.averageSpeed(),
                metrics.maxSpeed(),
                metrics.averageGAP(),
                metrics.averageHeartRate(),
                metrics.maxHeartRate(),
                metrics.minHeartRate(),
                metrics.hrZones(),
                metrics.hrZonesPercentage(),
                metrics.averageCadence(),
                metrics.maxCadence(),
                metrics.averageVerticalOscillation(),
                metrics.averageStanceTime(),
                metrics.averageVerticalRatio(),
                metrics.averageStepLength(),
                metrics.averagePower(),
                metrics.maxPower(),
                metrics.normalizedPower(),
                metrics.powerZones(),
                metrics.totalAscent(),
                metrics.totalDescent(),
                metrics.trainingEffect(),
                metrics.anaerobicTrainingEffect(),
                metrics.trainingLoadPeak(),
                metrics.workoutFeel(),
                metrics.workoutRpe(),
                laps,  // ← AÑADIDOS
                metrics.calculatedAt()
        );
    }

    /**
     * Calcula todas las métricas de una actividad.
     *
     * Este método orquesta todos los cálculos individuales y combina
     * datos extraídos del FIT con métricas calculadas.
     *
     * @param input datos normalizados de la actividad (del EP2)
     * @return métricas completas calculadas
     */
    public ActivityMetricsDto calculate(ActivityNormalizedDto input) {
        var session = input.session();

        // Calcular métricas de session
        Integer averagePace = calculatePace(session.totalTimerTime(), session.totalDistance());
        Integer averageGAP = calculateGAP(averagePace, session.totalAscent(), session.totalDistance());
        Integer minHeartRate = calculateMinHeartRate(input.samples());
        Map<String, Integer> hrZonesPercentage = calculateHrZonesPercentage(
                session.timeInHrZones(),
                session.totalTimerTime()
        );

        // Procesar laps
        List<LapMetricsDto> lapMetrics = input.laps().stream()
                .map(lapMetricsCalculator::calculate)
                .toList();

        // Calcular maxPace desde laps
        Integer maxPace = calculateMaxPaceFromLaps(lapMetrics);

        // Normalizar hrZones: null -> Map vacío (consistente con hrZonesPercentage)
        Map<String, Integer> hrZones = session.timeInHrZones() != null
                ? session.timeInHrZones()
                : Map.of();

        // Construir DTO con todo
        return new ActivityMetricsDto(
                input.activityId(),
                input.userId(),
                session.totalDistance(),
                session.totalTimerTime(),
                session.totalElapsedTime(),
                session.totalCalories(),
                averagePace,
                maxPace,  // ← Ahora tiene valor
                session.enhancedAvgSpeed(),
                session.enhancedMaxSpeed(),
                averageGAP,
                session.avgHeartRate(),
                session.maxHeartRate(),
                minHeartRate,
                hrZones,
                hrZonesPercentage,
                session.avgCadence(),
                session.maxCadence(),
                session.avgVerticalOscillation(),
                session.avgStanceTime(),
                session.avgVerticalRatio(),
                session.avgStepLength(),
                session.avgPower(),
                session.maxPower(),
                session.normalizedPower(),
                session.timeInPowerZones(),
                session.totalAscent(),
                session.totalDescent(),
                session.totalTrainingEffect(),
                session.totalAnaerobicTrainingEffect(),
                session.trainingLoadPeak(),
                session.workoutFeel(),
                session.workoutRpe(),
                lapMetrics,  // ← Ahora tiene los laps
                Instant.now()
        );
    }
}
