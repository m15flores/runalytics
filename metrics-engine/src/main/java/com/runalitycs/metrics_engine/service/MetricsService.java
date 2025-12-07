package com.runalitycs.metrics_engine.service;

import com.runalitycs.metrics_engine.dto.ActivityMetricsDto;
import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.dto.LapMetricsDto;
import com.runalitycs.metrics_engine.entity.ActivityMetrics;
import com.runalitycs.metrics_engine.entity.LapMetrics;
import com.runalitycs.metrics_engine.kafka.MetricsProducer;
import com.runalitycs.metrics_engine.repository.ActivityMetricsRepository;
import com.runalitycs.metrics_engine.repository.LapMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final ActivityMetricsCalculator activityCalculator;
    private final ActivityMetricsRepository activityRepository;
    private final LapMetricsRepository lapRepository;
    private final MetricsProducer producer;

    public MetricsService(
            ActivityMetricsCalculator activityCalculator,
            ActivityMetricsRepository activityRepository,
            LapMetricsRepository lapRepository,
            MetricsProducer producer) {
        this.activityCalculator = activityCalculator;
        this.activityRepository = activityRepository;
        this.lapRepository = lapRepository;
        this.producer = producer;
    }

    /**
     * Procesa una actividad normalizada:
     * 1. Calcula métricas
     * 2. Persiste en BD
     * 3. Publica a Kafka
     */
    @Transactional
    public void processActivity(ActivityNormalizedDto input) {
        log.info("Processing activity: {} for user: {}", input.activityId(), input.userId());

        try {
            ActivityMetricsDto activityMetricsDto = activityCalculator.calculate(input);
            log.info("Metrics calculated successfully");

            ActivityMetrics activityMetrics = toActivityMetricsEntity(activityMetricsDto);
            this.activityRepository.save(activityMetrics);
            log.info("Activity metrics saved to database");

            activityMetricsDto.laps().forEach(lap -> this.lapRepository.save(toLapMetricsEntity(lap, input.activityId())));
            log.info("Lap metrics saved to database");

            producer.publishMetrics(activityMetricsDto);
            log.info("Successfully processed activity: {}", input.activityId());
        } catch (Exception e) {
            log.error("Error processing activity: {}", input.activityId(), e);
            throw e;
        }
    }

    private ActivityMetrics toActivityMetricsEntity(ActivityMetricsDto dto) {
        ActivityMetrics entity = new ActivityMetrics();

        // Identificación
        entity.setActivityId(dto.activityId());
        entity.setUserId(dto.userId());

        // Basic metrics
        entity.setTotalDistance(dto.totalDistance());
        entity.setTotalDuration(dto.totalDuration());
        entity.setTotalElapsedTime(dto.totalElapsedTime());
        entity.setTotalCalories(dto.totalCalories());

        // Pace & Speed
        entity.setAveragePace(dto.averagePace());
        entity.setMaxPace(dto.maxPace());
        entity.setAverageSpeed(dto.averageSpeed());
        entity.setMaxSpeed(dto.maxSpeed());
        entity.setAverageGAP(dto.averageGAP());

        // Heart Rate
        entity.setAverageHeartRate(dto.averageHeartRate());
        entity.setMaxHeartRate(dto.maxHeartRate());
        entity.setMinHeartRate(dto.minHeartRate());
        entity.setHrZones(dto.hrZones());
        entity.setHrZonesPercentage(dto.hrZonesPercentage());

        // Cadence
        entity.setAverageCadence(dto.averageCadence());
        entity.setMaxCadence(dto.maxCadence());

        // Running Dynamics
        entity.setAverageVerticalOscillation(dto.averageVerticalOscillation());
        entity.setAverageStanceTime(dto.averageStanceTime());
        entity.setAverageVerticalRatio(dto.averageVerticalRatio());
        entity.setAverageStepLength(dto.averageStepLength());

        // Power
        entity.setAveragePower(dto.averagePower());
        entity.setMaxPower(dto.maxPower());
        entity.setNormalizedPower(dto.normalizedPower());
        entity.setPowerZones(dto.powerZones());

        // Elevation
        entity.setTotalAscent(dto.totalAscent());
        entity.setTotalDescent(dto.totalDescent());

        // Training Load
        entity.setTrainingEffect(dto.trainingEffect());
        entity.setAnaerobicTrainingEffect(dto.anaerobicTrainingEffect());
        entity.setTrainingLoadPeak(dto.trainingLoadPeak());

        // Subjective
        entity.setWorkoutFeel(dto.workoutFeel());
        entity.setWorkoutRpe(dto.workoutRpe());

        // Metadata
        entity.setCalculatedAt(dto.calculatedAt());

        return entity;
    }

    private LapMetrics toLapMetricsEntity(LapMetricsDto dto, UUID activityId) {
        LapMetrics entity = new LapMetrics();

        // Identificación
        entity.setActivityId(activityId);
        entity.setLapNumber(dto.lapNumber());
        entity.setLapName(dto.lapName());
        entity.setIntensity(dto.intensity());
        entity.setStartTime(dto.startTime());

        // Basic metrics
        entity.setDistance(dto.distance());
        entity.setDuration(dto.duration());
        entity.setCalories(dto.calories());

        // Pace & Speed
        entity.setAveragePace(dto.averagePace());
        entity.setMaxPace(dto.maxPace());
        entity.setAverageSpeed(dto.averageSpeed());
        entity.setMaxSpeed(dto.maxSpeed());
        entity.setAverageGAP(dto.averageGAP());

        // Heart Rate
        entity.setAverageHeartRate(dto.averageHeartRate());
        entity.setMaxHeartRate(dto.maxHeartRate());
        entity.setMinHeartRate(dto.minHeartRate());

        // Cadence
        entity.setAverageCadence(dto.averageCadence());
        entity.setMaxCadence(dto.maxCadence());

        // Running Dynamics
        entity.setAverageVerticalOscillation(dto.averageVerticalOscillation());
        entity.setAverageStanceTime(dto.averageStanceTime());
        entity.setAverageVerticalRatio(dto.averageVerticalRatio());
        entity.setAverageStepLength(dto.averageStepLength());

        // Power
        entity.setAveragePower(dto.averagePower());
        entity.setMaxPower(dto.maxPower());
        entity.setNormalizedPower(dto.normalizedPower());

        // Elevation
        entity.setTotalAscent(dto.totalAscent());
        entity.setTotalDescent(dto.totalDescent());

        return entity;
    }
}