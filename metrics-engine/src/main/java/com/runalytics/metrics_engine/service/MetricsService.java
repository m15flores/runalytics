package com.runalytics.metrics_engine.service;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.dto.ActivitySampleDto;
import com.runalytics.metrics_engine.entity.ActivityMetrics;
import com.runalytics.metrics_engine.entity.ActivitySample;
import com.runalytics.metrics_engine.entity.LapMetrics;
import com.runalytics.metrics_engine.kafka.MetricsProducer;
import com.runalytics.metrics_engine.mapper.ActivityMetricsMapper;
import com.runalytics.metrics_engine.mapper.LapMetricsMapper;
import com.runalytics.metrics_engine.repository.ActivityMetricsRepository;
import com.runalytics.metrics_engine.repository.ActivitySampleRepository;
import com.runalytics.metrics_engine.repository.LapMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private static final int MAX_SAMPLES = 500;

    private final ActivityMetricsCalculator activityCalculator;
    private final ActivityMetricsRepository activityRepository;
    private final LapMetricsRepository lapRepository;
    private final ActivitySampleRepository sampleRepository;
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

            if (input.samples() != null && !input.samples().isEmpty()) {
                persistSamples(input.samples(), input.activityId(), now);
                log.info("Saved {} samples to database", input.samples().size());
            }

            producer.publishMetrics(activityMetricsDto);
            log.info("Successfully processed activity: {}", input.activityId());
        } catch (Exception e) {
            log.error("Error processing activity: {}", input.activityId(), e);
            throw e;
        }
    }

    public List<ActivitySampleDto> getSamples(UUID activityId) {
        List<ActivitySample> all = sampleRepository.findByActivityIdOrderByTimestampAsc(activityId);
        return downsample(all, MAX_SAMPLES).stream()
                .map(s -> new ActivitySampleDto(
                        s.getTimestamp(),
                        s.getLatitude(),
                        s.getLongitude(),
                        s.getHeartRate(),
                        s.getCadence(),
                        s.getAltitude(),
                        s.getSpeed(),
                        s.getPower(),
                        s.getDistance()))
                .toList();
    }

    private List<ActivitySample> downsample(List<ActivitySample> samples, int maxPoints) {
        if (samples.size() <= maxPoints) {
            return samples;
        }
        int step = samples.size() / maxPoints;
        List<ActivitySample> result = new ArrayList<>(maxPoints);
        for (int i = 0; i < samples.size() && result.size() < maxPoints; i += step) {
            result.add(samples.get(i));
        }
        return result;
    }

    private void persistSamples(List<ActivityNormalizedDto.SampleData> samples, UUID activityId, Instant now) {
        List<ActivitySample> entities = samples.stream()
                .map(s -> {
                    ActivitySample e = new ActivitySample();
                    e.setActivityId(activityId);
                    e.setTimestamp(s.timestamp());
                    e.setLatitude(s.latitude());
                    e.setLongitude(s.longitude());
                    e.setHeartRate(s.heartRate());
                    e.setCadence(s.cadence());
                    e.setAltitude(s.altitude());
                    e.setSpeed(s.speed());
                    e.setPower(s.power());
                    e.setDistance(s.distance());
                    e.setCreatedAt(now);
                    return e;
                })
                .toList();
        sampleRepository.saveAll(entities);
    }
}