package com.runalytics.activity.service;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.kafka.ActivityProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityProducer activityProducer;
    private final Clock clock;

    public String ingestActivity(ActivityDto dto) {
        log.info("action=ingest userId={}", dto.userId());

        activityProducer.publishActivity(dto);

        return dto.userId();
    }

    public String ingestFitFile(String userId, String device, String source, byte[] fitBytes) {
        log.info("action=ingestFit userId={} sizeBytes={}", userId, fitBytes.length);

        String base64 = Base64.getEncoder().encodeToString(fitBytes);
        ActivityDto dto = new ActivityDto(userId, device, Instant.now(clock), source, Map.of("fitBase64", base64));
        activityProducer.publishActivity(dto);

        log.info("action=ingestFit status=success userId={}", userId);
        return userId;
    }
}