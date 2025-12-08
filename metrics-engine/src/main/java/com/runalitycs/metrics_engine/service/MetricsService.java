package com.runalitycs.metrics_engine.service;

import com.runalitycs.metrics_engine.dto.ActivityMetricsDto;
import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.entity.ActivityMetrics;
import com.runalitycs.metrics_engine.entity.LapMetrics;
import com.runalitycs.metrics_engine.kafka.MetricsProducer;
import com.runalitycs.metrics_engine.mapper.ActivityMetricsMapper;
import com.runalitycs.metrics_engine.mapper.LapMetricsMapper;
import com.runalitycs.metrics_engine.repository.ActivityMetricsRepository;
import com.runalitycs.metrics_engine.repository.LapMetricsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final ActivityMetricsCalculator activityCalculator;
    private final ActivityMetricsRepository activityRepository;
    private final LapMetricsRepository lapRepository;
    private final MetricsProducer producer;
    private final ActivityMetricsMapper activityMapper;
    private final LapMetricsMapper lapMapper;

    public MetricsService(
            ActivityMetricsCalculator activityCalculator,
            ActivityMetricsRepository activityRepository,
            LapMetricsRepository lapRepository,
            MetricsProducer producer,
            ActivityMetricsMapper activityMapper,
            LapMetricsMapper lapMapper) {
        this.activityCalculator = activityCalculator;
        this.activityRepository = activityRepository;
        this.lapRepository = lapRepository;
        this.producer = producer;
        this.activityMapper = activityMapper;
        this.lapMapper = lapMapper;
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
            // 1. Calculate metrics
            ActivityMetricsDto activityMetricsDto = activityCalculator.calculate(input);
            log.info("Metrics calculated successfully");

            // 2. Save activity metrics using mapper
            ActivityMetrics activityMetrics = activityMapper.toEntity(activityMetricsDto);
            activityRepository.save(activityMetrics);
            log.info("Activity metrics saved to database");

            // 3. Save lap metrics using mapper
            if (activityMetricsDto.laps() != null && !activityMetricsDto.laps().isEmpty()) {
                List<LapMetrics> lapEntities = lapMapper.toEntityList(activityMetricsDto.laps());
                // Set activityId for each lap (ignored by mapper)
                lapEntities.forEach(lap -> lap.setActivityId(input.activityId()));
                lapRepository.saveAll(lapEntities);
                log.info("Saved {} lap metrics to database", lapEntities.size());
            }

            // 4. Publish to Kafka
            producer.publishMetrics(activityMetricsDto);
            log.info("Successfully processed activity: {}", input.activityId());
        } catch (Exception e) {
            log.error("Error processing activity: {}", input.activityId(), e);
            throw e;
        }
    }
}