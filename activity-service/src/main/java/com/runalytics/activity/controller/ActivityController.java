package com.runalytics.activity.controller;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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

    @PostMapping(value = "/fit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> ingestFit(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "device", required = false) String device,
            @RequestParam(value = "source", required = false) String source) throws IOException {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId cannot be blank");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("FIT file cannot be empty");
        }

        log.info("action=ingestFit userId={} filename={}", userId, file.getOriginalFilename());

        String resultUserId = activityService.ingestFitFile(userId, device, source, file.getBytes());
        log.info("action=ingestFit status=success userId={}", resultUserId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("userId", resultUserId));
    }
}