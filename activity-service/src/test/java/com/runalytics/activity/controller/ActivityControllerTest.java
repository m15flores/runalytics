package com.runalytics.activity.controller;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityController.class)
public class ActivityControllerTest {

    private static final String VALID_REQUEST_BODY = """
            {
                "userId": "user-12345",
                "device": "Garmin-Fenix-7-Pro",
                "timestamp": "2025-01-01T10:30:00Z",
                "source": "garmin-mock",
                "raw": {
                    "distance_m": 10042,
                    "duration_s": 2780
                }
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    void shouldIngestActivityAndReturn201Created() throws Exception {
        when(activityService.ingestActivity(any(ActivityDto.class))).thenReturn("user-12345");

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-12345"));

        verify(activityService, times(1)).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenUserIdIsMissing() throws Exception {
        String requestBody = """
                {
                    "device": "Garmin-Fenix-7-Pro",
                    "timestamp": "2025-01-01T10:30:00Z",
                    "source": "garmin-mock",
                    "raw": {
                        "distance_m": 10042,
                        "duration_s": 2780
                    }
                }
                """;

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").value("userId cannot be blank"));

        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenTimestampIsNull() throws Exception {
        String requestBody = """
                {
                    "userId": "user-12345",
                    "device": "Garmin-Fenix-7-Pro",
                    "timestamp": null,
                    "source": "garmin-mock",
                    "raw": {
                        "distance_m": 10042,
                        "duration_s": 2780
                    }
                }
                """;

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.timestamp").value("timestamp cannot be null"));

        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenRawDataIsEmpty() throws Exception {
        String requestBody = """
                {
                    "userId": "user-12345",
                    "device": "Garmin-Fenix-7-Pro",
                    "timestamp": "2025-01-01T10:30:00Z",
                    "source": "garmin-mock",
                    "raw": {}
                }
                """;

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.raw").value("raw data cannot be empty"));

        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        String requestBody = """
                {
                    "userId": ,
                }
                """;

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON"))
                .andExpect(jsonPath("$.message").value("Invalid JSON format"));

        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        when(activityService.ingestActivity(any(ActivityDto.class))).thenThrow(new RuntimeException("Kafka unavailable"));

        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Kafka unavailable"));

        verify(activityService, times(1)).ingestActivity(any(ActivityDto.class));
    }
}