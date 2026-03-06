package com.runalytics.ai_coach.controller;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.RecommendationType;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.exception.GlobalExceptionHandler;
import com.runalytics.ai_coach.mapper.RecommendationMapperImpl;
import com.runalytics.ai_coach.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@Import({RecommendationMapperImpl.class, GlobalExceptionHandler.class})
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private Clock clock;

    @Test
    void shouldGetActiveRecommendationsForUser() throws Exception {
        // Given
        String userId = "mario-runner";
        List<Recommendation> recommendations = List.of(
                Recommendation.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .userId(userId)
                        .reportId(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .type(RecommendationType.ZONE_COMPLIANCE)
                        .priority(Priority.HIGH)
                        .content("You ran in Z3 for 40% of the session")
                        .rationale("Z2 compliance is non-negotiable during aerobic base")
                        .verdict(TrainingVerdict.INVALID)
                        .weekInCycle(2)
                        .applied(false)
                        .createdAt(Instant.parse("2026-03-06T10:00:00Z"))
                        .build()
        );
        when(recommendationService.getActiveRecommendations(userId)).thenReturn(recommendations);

        // When & Then
        mockMvc.perform(get("/api/recommendations/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].type").value("ZONE_COMPLIANCE"))
                .andExpect(jsonPath("$[0].priority").value("HIGH"))
                .andExpect(jsonPath("$[0].verdict").value("INVALID"))
                .andExpect(jsonPath("$[0].weekInCycle").value(2));
    }

    @Test
    void shouldReturnEmptyListWhenNoActiveRecommendations() throws Exception {
        // Given
        when(recommendationService.getActiveRecommendations("new-user")).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/recommendations/users/{userId}", "new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}