package com.runalytics.activity.controller;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.service.ActivityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    // --- POST /activities/fit ---

    @Test
    void shouldReturn201WhenValidFitFileUploaded() throws Exception {
        byte[] fitBytes = new byte[]{1, 2, 3, 4, 5};
        MockMultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", fitBytes);

        when(activityService.ingestFitFile(eq("mario-001"), eq("Garmin Fenix"), isNull(), any(byte[].class)))
                .thenReturn("mario-001");

        mockMvc.perform(multipart("/activities/fit")
                        .file(file)
                        .param("userId", "mario-001")
                        .param("device", "Garmin Fenix"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("mario-001"));

        verify(activityService, times(1)).ingestFitFile(eq("mario-001"), eq("Garmin Fenix"), isNull(), any(byte[].class));
    }

    @Test
    void shouldReturn400WhenFitFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/activities/fit")
                        .file(file)
                        .param("userId", "mario-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(activityService, never()).ingestFitFile(any(), any(), any(), any());
    }

    @Test
    void shouldReturn400WhenFitUserIdIsBlank() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/activities/fit")
                        .file(file)
                        .param("userId", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));

        verify(activityService, never()).ingestFitFile(any(), any(), any(), any());
    }

    @Test
    void shouldReturn500WhenFitServiceThrowsException() throws Exception {
        byte[] fitBytes = new byte[]{1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "activity.fit", "application/octet-stream", fitBytes);

        when(activityService.ingestFitFile(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Kafka unavailable"));

        mockMvc.perform(multipart("/activities/fit")
                        .file(file)
                        .param("userId", "mario-001"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Kafka unavailable"));
    }
}