package com.runalitycs.activity.controller;

import com.runalitycs.activity.dto.ActivityDto;
import com.runalitycs.activity.service.ActivityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/activities")
public class ActivityController {

    private static final Logger log = LoggerFactory.getLogger(ActivityController.class);
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @PostMapping
    public ResponseEntity<?> ingest(@Valid @RequestBody ActivityDto activityDto) {
        log.info("Received activity ingestion request for user: {}", activityDto.userId());

        String userId = activityService.ingestActivity(activityDto);
        log.info("Activity ingested successfully for user: {}", userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("userId", userId));
    }
}
