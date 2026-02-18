package com.runalytics.activity.service;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.kafka.ActivityProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityProducer activityProducer;

    public String ingestActivity(ActivityDto dto) {
        log.info("action=ingest userId={}", dto.userId());

        activityProducer.publishActivity(dto);

        return dto.userId();
    }
}