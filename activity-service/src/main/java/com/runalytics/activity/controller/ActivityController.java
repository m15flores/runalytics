package com.runalytics.activity.controller;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping
    public ResponseEntity<Map<String, String>> ingest(@Valid @RequestBody ActivityDto activityDto) {
        log.info("action=ingest userId={}", activityDto.userId());

        String userId = activityService.ingestActivity(activityDto);
        log.info("action=ingest status=success userId={}", userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("userId", userId));
    }
}