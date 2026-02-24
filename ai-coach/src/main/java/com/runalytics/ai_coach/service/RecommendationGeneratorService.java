package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import com.runalytics.ai_coach.dto.openai.AiRecommendationResponse;
import com.runalytics.ai_coach.repository.RecommendationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to generate AI-powered recommendations from training reports
 */
@Service
@Slf4j
public class RecommendationGeneratorService {

    private final OpenAiApiService openAiApiService;
    private final PromptTemplateService promptTemplateService;
    private final RecommendationRepository recommendationRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final int expirationDays;

    public RecommendationGeneratorService(
            OpenAiApiService openAiApiService,
            PromptTemplateService promptTemplateService,
            RecommendationRepository recommendationRepository,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${app.recommendations.expiration-days:7}") int expirationDays) {
        this.openAiApiService = openAiApiService;
        this.promptTemplateService = promptTemplateService;
        this.recommendationRepository = recommendationRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.expirationDays = expirationDays;
    }

    /**
     * Generate recommendations for a training report using AI
     *
     * @param report Training report to analyze
     * @param cycleContext Current training cycle context
     * @return List of generated recommendations
     */
    @Transactional
    public List<Recommendation> generateRecommendations(
            TrainingReportDto report,
            TrainingCycleContext cycleContext) {

        log.info("Generating recommendations for report: {}, userId: {}, week: {}/{}",
                report.getId(), report.getUserId(), report.getWeekNumber(), report.getYear());

        try {
            // Step 1: Build prompts
            String systemPrompt = promptTemplateService.buildSystemPrompt(cycleContext);
            String userPrompt = promptTemplateService.buildUserPrompt(report, cycleContext);

            // Step 2: Call OpenAI
            String aiResponseJson = openAiApiService.analyze(systemPrompt, userPrompt);

            // Step 3: Parse response
            AiRecommendationResponse aiResponse = objectMapper.readValue(
                    aiResponseJson,
                    AiRecommendationResponse.class
            );

            log.info("AI verdict: {}, recommendations count: {}",
                    aiResponse.getVerdict(),
                    aiResponse.getRecommendations() != null ? aiResponse.getRecommendations().size() : 0);

            // Step 4: Convert to domain entities
            List<Recommendation> recommendations = aiResponse.getRecommendations().stream()
                    .map(aiRec -> convertToRecommendation(aiRec, report, cycleContext, aiResponse))
                    .collect(Collectors.toList());

            // Step 5: Save to database
            List<Recommendation> saved = recommendationRepository.saveAll(recommendations);

            log.info("Successfully generated and saved {} recommendations for report: {}",
                    saved.size(), report.getId());

            return saved;

        } catch (Exception e) {
            log.error("Error generating recommendations for report: {}, error: {}",
                    report.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate recommendations", e);
        }
    }

    /**
     * Convert AI recommendation to domain entity
     */
    private Recommendation convertToRecommendation(
            AiRecommendationResponse.AiRecommendation aiRec,
            TrainingReportDto report,
            TrainingCycleContext cycleContext,
            AiRecommendationResponse aiResponse) {

        return Recommendation.builder()
                .userId(report.getUserId())
                .reportId(report.getId())
                .type(parseRecommendationType(aiRec.getType()))
                .priority(parsePriority(aiRec.getPriority()))
                .content(aiRec.getContent())
                .rationale(aiRec.getRationale())
                .verdict(aiResponse.getVerdict())
                .weekInCycle(cycleContext.getWeekInCycle())
                .trainingPhase(cycleContext.getPhase())
                .createdAt(Instant.now(clock))
                .expiresAt(Instant.now(clock).plus(expirationDays, ChronoUnit.DAYS))
                .applied(false)
                .metadata(String.format("{\"category\": \"%s\", \"verdict_rationale\": \"%s\"}",
                        aiRec.getCategory(),
                        aiResponse.getVerdictRationale()))
                .build();
    }

    /**
     * Parse recommendation type from AI response
     */
    private RecommendationType parseRecommendationType(String type) {
        try {
            return RecommendationType.valueOf(type);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown recommendation type: {}, defaulting to WORKOUT_QUALITY", type);
            return RecommendationType.WORKOUT_QUALITY;
        }
    }

    /**
     * Parse priority from AI response
     */
    private Priority parsePriority(String priority) {
        try {
            return Priority.valueOf(priority);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown priority: {}, defaulting to MEDIUM", priority);
            return Priority.MEDIUM;
        }
    }
}
