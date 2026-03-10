package com.runalytics.metrics_engine.controller;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import com.runalytics.metrics_engine.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MetricsController.class)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @Test
    void shouldReturn200WithMetricsWhenActivityFound() throws Exception {
        // Given
        UUID activityId = UUID.randomUUID();
        ActivityMetricsDto dto = buildTestDto(activityId);
        when(metricsService.getActivityMetrics(activityId)).thenReturn(Optional.of(dto));

        // When / Then
        mockMvc.perform(get("/activities/{activityId}/metrics", activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    void shouldReturn404WhenActivityNotFound() throws Exception {
        // Given
        UUID activityId = UUID.randomUUID();
        when(metricsService.getActivityMetrics(activityId)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/activities/{activityId}/metrics", activityId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn200WithLatestMetricsWhenFound() throws Exception {
        // Given
        String userId = "demo";
        UUID activityId = UUID.randomUUID();
        ActivityMetricsDto dto = buildTestDto(activityId);
        when(metricsService.getLatestActivityMetrics(userId)).thenReturn(Optional.of(dto));

        // When / Then
        mockMvc.perform(get("/activities/users/{userId}/latest", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").value(activityId.toString()))
                .andExpect(jsonPath("$.userId").value("user-1"));
    }

    @Test
    void shouldReturn404WhenNoActivityForUser() throws Exception {
        // Given
        String userId = "demo";
        when(metricsService.getLatestActivityMetrics(userId)).thenReturn(Optional.empty());

        // When / Then
        mockMvc.perform(get("/activities/users/{userId}/latest", userId))
                .andExpect(status().isNotFound());
    }

    // --- test fixtures ---

    private ActivityMetricsDto buildTestDto(UUID activityId) {
        return new ActivityMetricsDto(
                activityId, "user-1", null,
                null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                null, null,
                List.of(), null
        );
    }
}