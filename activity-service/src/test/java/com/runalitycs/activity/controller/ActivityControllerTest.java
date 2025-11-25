package com.runalitycs.activity.controller;

import com.runalitycs.activity.dto.ActivityDto;
import com.runalitycs.activity.service.ActivityService;
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

@WebMvcTest
public class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityService activityService;

    @Test
    void shouldIngestActivityAndReturn201Created() throws Exception {
        //Given
        String requestBody = """
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
        when(activityService.ingestActivity(any(ActivityDto.class))).thenReturn("user-12345");

        //When
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-12345"));

        //Verify
        verify(activityService, times(1)).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenUserIdIsMissing() throws Exception {
        //Given
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

        //When
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.userId").value("userId cannot be blank"));

        //Verify
        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenTimestampIsNull() throws Exception {
        //Given
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

        //When & Then
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.timestamp").value("timestamp cannot be null"));

        //Verify
        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenRawDataIsEmpty() throws Exception {
        //Given
        String requestBody = """
        {
            "userId": "user-12345",
            "device": "Garmin-Fenix-7-Pro",
            "timestamp": "2025-01-01T10:30:00Z",
            "source": "garmin-mock",
            "raw": {}
        }
        """;

        //When & Then
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.raw").value("raw data cannot be empty"));

        //Verify
        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn400WhenJsonIsMalformed() throws Exception {
        //Given
        String requestBody = """
        {
            "userId": ,
        }
        """;

        //When & Then
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON"))
                .andExpect(jsonPath("$.message").value("Invalid JSON format"));

        //Verify
        verify(activityService, never()).ingestActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        //Given
        String requestBody = """
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
        when(activityService.ingestActivity(any(ActivityDto.class))).thenThrow(new RuntimeException("Kafka unavailable"));

        //When & Then
        mockMvc.perform(post("/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Kafka unavailable"));

        //Verify
        verify(activityService, times(1)).ingestActivity(any(ActivityDto.class));
    }
}
