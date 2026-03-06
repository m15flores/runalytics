package com.runalytics.ai_coach.service;

import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final RecommendationRepository repository;
    private final Clock clock;

    public List<Recommendation> getActiveRecommendations(String userId) {
        log.info("action=getActiveRecommendations userId={}", userId);
        return repository.findActiveRecommendationsByUserId(userId, Instant.now(clock));
    }
}