package com.runalytics.normalizer.service;

import com.runalytics.normalizer.dto.ActivityNormalizedDto;
import com.runalytics.normalizer.dto.ParsedFitData;
import com.runalytics.normalizer.entity.Activity;
import com.runalytics.normalizer.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityNormalizerService {

    private final ActivityRepository activityRepository;
    private final Clock clock;

    @Transactional
    public ActivityNormalizedDto normalize(String userId, String device, ParsedFitData parsedData) {
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