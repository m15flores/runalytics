package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import com.runalytics.metrics_engine.entity.LapMetrics;
import com.runalytics.metrics_engine.kafka.MetricsProducer;
import com.runalytics.metrics_engine.mapper.ActivityMetricsMapper;
import com.runalytics.metrics_engine.mapper.LapMetricsMapper;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final ActivityMetricsCalculator activityCalculator;
    private final ActivityMetricsRepository activityRepository;
    private final LapMetricsRepository lapRepository;
    private final MetricsProducer producer;
    private final ActivityMetricsMapper activityMapper;
    private final LapMetricsMapper lapMapper;
    private final Clock clock;

    public Optional<ActivityMetricsDto> getActivityMetrics(UUID activityId) {
        return activityRepository.findByActivityId(activityId)
                .map(entity -> {
                    List<LapMetrics> laps = lapRepository.findByActivityIdOrderByLapNumberAsc(activityId);
                    return activityMapper.toFullDto(entity, lapMapper.toDtoList(laps));
                });
    }

    public Optional<ActivityMetricsDto> getLatestActivityMetrics(String userId) {
        return activityRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(entity -> {
                    List<LapMetrics> laps = lapRepository.findByActivityIdOrderByLapNumberAsc(entity.getActivityId());
                    return activityMapper.toFullDto(entity, lapMapper.toDtoList(laps));
                });
    }

    @Transactional
    public void processActivity(ActivityNormalizedDto input) {
        log.info("Processing activity: {} for user: {}", input.activityId(), input.userId());

        if (activityRepository.existsByActivityId(input.activityId())) {
            log.warn("activity=already-processed activityId={}", input.activityId());
            return;
        }

        try {
            ActivityMetricsDto activityMetricsDto = activityCalculator.calculate(input);
            log.info("Metrics calculated successfully");

            Instant now = Instant.now(clock);

            ActivityMetrics activityMetrics = activityMapper.toEntity(activityMetricsDto);
            activityMetrics.setCreatedAt(now);
            activityMetrics.setUpdatedAt(now);
            activityRepository.save(activityMetrics);
            log.info("Activity metrics saved to database");

            if (activityMetricsDto.laps() != null && !activityMetricsDto.laps().isEmpty()) {
                List<LapMetrics> lapEntities = lapMapper.toEntityList(activityMetricsDto.laps());
                lapEntities.forEach(lap -> {
                    lap.setActivityId(input.activityId());
                    lap.setCreatedAt(now);
                    lap.setUpdatedAt(now);
                });
                lapRepository.saveAll(lapEntities);
                log.info("Saved {} lap metrics to database", lapEntities.size());
            }

            producer.publishMetrics(activityMetricsDto);
            log.info("Successfully processed activity: {}", input.activityId());
        } catch (Exception e) {
            log.error("Error processing activity: {}", input.activityId(), e);
            throw e;
        }
    }
}