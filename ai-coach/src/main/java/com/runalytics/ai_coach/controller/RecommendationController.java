package com.runalytics.ai_coach.controller;

import com.runalytics.ai_coach.dto.RecommendationDto;
import com.runalytics.ai_coach.mapper.RecommendationMapper;
import com.runalytics.ai_coach.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@Slf4j
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationMapper mapper;

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<RecommendationDto>> getActiveRecommendations(@PathVariable String userId) {
        log.info("GET /api/recommendations/users/{} - Fetching active recommendations", userId);

        List<RecommendationDto> dtos = recommendationService.getActiveRecommendations(userId).stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }
}