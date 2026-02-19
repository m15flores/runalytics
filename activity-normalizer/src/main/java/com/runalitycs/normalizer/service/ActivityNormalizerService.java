package com.runalitycs.normalizer.service;

import com.runalitycs.normalizer.dto.ActivityNormalizedDto;
import com.runalitycs.normalizer.dto.ParsedFitData;
import com.runalitycs.normalizer.entity.Activity;
import com.runalitycs.normalizer.repository.ActivityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ActivityNormalizerService {

    private static final Logger log = LoggerFactory.getLogger(ActivityNormalizerService.class);

    private final ActivityRepository activityRepository;
    private final Clock clock;

    public ActivityNormalizerService(ActivityRepository activityRepository, Clock clock) {
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    @Transactional
    public ActivityNormalizedDto normalize(String userId, String device, ParsedFitData parsedData) {
        // Validaciones
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be null or blank");
        }
        if (parsedData == null) {
            throw new IllegalArgumentException("parsedData cannot be null");
        }

        log.info("Normalizing activity for user: {}", userId);

        Instant now = Instant.now(clock);

        Activity activity = new Activity();
        activity.setUserId(userId);
        activity.setDevice(device);
        activity.setStartedAt(parsedData.startedAt());
        activity.setDurationSeconds(parsedData.durationSeconds());
        activity.setDistanceMeters(parsedData.distanceMeters());
        activity.setSamples(parsedData.samples());
        activity.setCreatedAt(now);
        activity.setUpdatedAt(now);

        Activity savedActivity = activityRepository.save(activity);

        log.info("Activity saved with ID: {} for user: {}", savedActivity.getId(), userId);

        // Generar DTO normalizado para Kafka
        ActivityNormalizedDto dto = new ActivityNormalizedDto(
                savedActivity.getId(),
                savedActivity.getUserId(),
                savedActivity.getDevice(),
                savedActivity.getStartedAt(),
                savedActivity.getDurationSeconds(),
                savedActivity.getDistanceMeters(),
                savedActivity.getSamples(),
                Instant.now(clock)
        );

        return dto;
    }
}