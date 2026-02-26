package com.runalytics.normalizer.service;

import com.runalytics.normalizer.dto.ActivityNormalizedDto;
import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import com.runalytics.normalizer.entity.Activity;
import com.runalytics.normalizer.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

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

        log.info("action=normalize userId={}", userId);

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

        log.info("action=normalize status=saved activityId={} userId={}", savedActivity.getId(), userId);

        ActivityNormalizedDto.SessionData sessionData = buildSessionData(parsedData.session());
        List<ActivityNormalizedDto.LapData> lapDataList = buildLapDataList(parsedData.laps());
        List<ActivityNormalizedDto.SampleData> sampleDataList = buildSampleDataList(parsedData.samples());

        return new ActivityNormalizedDto(
                savedActivity.getId(),
                savedActivity.getUserId(),
                savedActivity.getDevice(),
                savedActivity.getStartedAt(),
                sessionData,
                lapDataList,
                sampleDataList,
                Instant.now(clock)
        );
    }

    private ActivityNormalizedDto.SessionData buildSessionData(ParsedFitData.SessionInfo s) {
        if (s == null) {
            return null;
        }
        return new ActivityNormalizedDto.SessionData(
                s.totalDistance(), s.totalTimerTime(), s.totalElapsedTime(), s.totalCalories(),
                s.avgHeartRate(), s.maxHeartRate(), s.avgCadence(), s.maxCadence(),
                s.enhancedAvgSpeed(), s.enhancedMaxSpeed(),
                s.avgPower(), s.maxPower(), s.normalizedPower(),
                s.avgVerticalOscillation(), s.avgStanceTime(), s.avgVerticalRatio(), s.avgStepLength(),
                s.totalAscent(), s.totalDescent(),
                s.totalTrainingEffect(), s.totalAnaerobicTrainingEffect(), s.trainingLoadPeak(),
                s.workoutFeel(), s.workoutRpe(),
                s.timeInHrZones(), s.timeInPowerZones(),
                s.maxHeartRateConfig(), s.restingHeartRate(), s.thresholdHeartRate(), s.functionalThresholdPower()
        );
    }

    private List<ActivityNormalizedDto.LapData> buildLapDataList(List<ParsedFitData.LapInfo> laps) {
        if (laps == null || laps.isEmpty()) {
            return List.of();
        }
        return laps.stream()
                .map(lap -> new ActivityNormalizedDto.LapData(
                        lap.lapNumber(), lap.startTime(),
                        lap.totalDistance(), lap.totalTimerTime(), lap.totalElapsedTime(), lap.totalCalories(),
                        lap.avgHeartRate(), lap.maxHeartRate(), lap.avgCadence(), lap.maxCadence(),
                        lap.enhancedAvgSpeed(), lap.enhancedMaxSpeed(),
                        lap.avgPower(), lap.maxPower(), lap.normalizedPower(),
                        lap.avgVerticalOscillation(), lap.avgStanceTime(), lap.avgVerticalRatio(), lap.avgStepLength(),
                        lap.totalAscent(), lap.totalDescent(),
                        lap.intensity(), lap.wktStepIndex()
                ))
                .toList();
    }

    private List<ActivityNormalizedDto.SampleData> buildSampleDataList(List<ActivitySample> samples) {
        if (samples == null || samples.isEmpty()) {
            return List.of();
        }
        return samples.stream()
                .map(s -> new ActivityNormalizedDto.SampleData(
                        s.timestamp(), s.latitude(), s.longitude(),
                        s.heartRate(), s.cadence(), s.altitude(),
                        s.speed(), s.power(), s.distance()
                ))
                .toList();
    }
}