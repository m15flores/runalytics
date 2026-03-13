package com.runalitycs.report_generator.controller;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.exception.GlobalExceptionHandler;
import com.runalitycs.report_generator.service.TrainingReportService;
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

@WebMvcTest(TrainingReportController.class)
@Import({GlobalExceptionHandler.class})
class TrainingReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TrainingReportService trainingReportService;

    @MockitoBean
    private Clock clock;

    @Test
    void shouldGetReportsByUserId() throws Exception {
        // Given
        String userId = "mario-runner";
        List<TrainingReportDto> reports = List.of(
                TrainingReportDto.builder()
                        .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                        .userId(userId)
                        .weekNumber(10)
                        .year(2026)
                        .markdownContent("# Report week 10")
                        .createdAt(Instant.parse("2026-03-06T10:00:00Z"))
                        .build(),
                TrainingReportDto.builder()
                        .id(UUID.fromString("22222222-2222-2222-2222-222222222222"))
                        .userId(userId)
                        .weekNumber(9)
                        .year(2026)
                        .markdownContent("# Report week 9")
                        .createdAt(Instant.parse("2026-02-27T10:00:00Z"))
                        .build()
        );
        when(trainingReportService.getReportsByUserId(userId)).thenReturn(reports);

        // When & Then
        mockMvc.perform(get("/api/reports/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].weekNumber").value(10))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[1].weekNumber").value(9));
    }

    @Test
    void shouldReturnEmptyListWhenNoReportsForUser() throws Exception {
        // Given
        when(trainingReportService.getReportsByUserId("new-user")).thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/reports/users/{userId}", "new-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldGetReportByUserIdAndWeek() throws Exception {
        // Given
        String userId = "mario-runner";
        TrainingReportDto report = TrainingReportDto.builder()
                .id(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .userId(userId)
                .weekNumber(10)
                .year(2026)
                .markdownContent("# Training Report - Week 10/2026")
                .summaryJson("{\"totalKm\": 42.0}")
                .createdAt(Instant.parse("2026-03-06T10:00:00Z"))
                .build();
        when(trainingReportService.getReportByUserIdAndWeek(userId, 10, 2026)).thenReturn(report);

        // When & Then
        mockMvc.perform(get("/api/reports/users/{userId}/{weekNumber}/{year}", userId, 10, 2026))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.weekNumber").value(10))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.markdownContent").value("# Training Report - Week 10/2026"))
                .andExpect(jsonPath("$.summaryJson").value("{\"totalKm\": 42.0}"));
    }

    @Test
    void shouldReturn404WhenReportNotFound() throws Exception {
        // Given
        String userId = "mario-runner";
        when(trainingReportService.getReportByUserIdAndWeek(userId, 99, 2026))
                .thenThrow(new IllegalArgumentException("Report not found for userId: mario-runner, week: 99, year: 2026"));

        // When & Then
        mockMvc.perform(get("/api/reports/users/{userId}/{weekNumber}/{year}", userId, 99, 2026))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
}