package com.runalytics.activity.service;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.kafka.ActivityProducer;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ActivityService {

    private static final Logger log = LoggerFactory.getLogger(ActivityService.class);

    private final ActivityProducer activityProducer;

    public ActivityService(ActivityProducer activityProducer) {
        this.activityProducer = activityProducer;
    }

    public String ingestActivity(ActivityDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("ActivityDto cannot be null");
        }

        log.info("Ingesting activity for user: {}", dto.userId());

        activityProducer.publishActivity(dto);

        return dto.userId();
    }
}
