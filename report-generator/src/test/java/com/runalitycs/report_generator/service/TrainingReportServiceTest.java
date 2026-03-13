package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.entity.TrainingReport;
import com.runalitycs.report_generator.repository.AthleteProfileRepository;
import com.runalitycs.report_generator.repository.TrainingReportRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingReportServiceTest {

    @Mock
    private TrainingReportRepository repository;

    @Mock
    private AthleteProfileRepository athleteProfileRepository;

    @InjectMocks
    private TrainingReportService service;

    @Test
    void shouldReturnReportsForUser() {
        // Given
        String userId = "mario-runner";
        List<TrainingReport> reports = List.of(
                TrainingReport.builder().id(UUID.randomUUID()).userId(userId).weekNumber(10).year(2026).build(),
                TrainingReport.builder().id(UUID.randomUUID()).userId(userId).weekNumber(9).year(2026).build()
        );
        when(repository.findByUserIdOrderByYearDescWeekNumberDesc(userId)).thenReturn(reports);
        when(athleteProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        List<TrainingReportDto> result = service.getReportsByUserId(userId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).weekNumber()).isEqualTo(10);
        assertThat(result.get(1).weekNumber()).isEqualTo(9);
    }

    @Test
    void shouldReturnEmptyListWhenNoReportsForUser() {
        // Given
        String userId = "new-user";
        when(repository.findByUserIdOrderByYearDescWeekNumberDesc(userId)).thenReturn(List.of());

        // When
        List<TrainingReportDto> result = service.getReportsByUserId(userId);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnReportByUserIdAndWeek() {
        // Given
        String userId = "mario-runner";
        TrainingReport report = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .weekNumber(10)
                .year(2026)
                .markdownContent("# Training Report - Week 10/2026")
                .build();
        when(repository.findByUserIdAndWeekNumberAndYear(userId, 10, 2026)).thenReturn(Optional.of(report));
        when(athleteProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When
        TrainingReportDto result = service.getReportByUserIdAndWeek(userId, 10, 2026);

        // Then
        assertThat(result.weekNumber()).isEqualTo(10);
        assertThat(result.year()).isEqualTo(2026);
        assertThat(result.markdownContent()).contains("Week 10/2026");
    }

    @Test
    void shouldEnrichDtoWithAthleteProfileWhenPresent() {
        // Given
        String userId = "mario-runner";
        TrainingReport report = TrainingReport.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .weekNumber(10)
                .year(2026)
                .build();
        AthleteProfile profile = AthleteProfile.builder()
                .userId(userId)
                .name("Mario Runner")
                .currentGoal("Run sub-4h marathon")
                .build();
        when(repository.findByUserIdAndWeekNumberAndYear(userId, 10, 2026)).thenReturn(Optional.of(report));
        when(athleteProfileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // When
        TrainingReportDto result = service.getReportByUserIdAndWeek(userId, 10, 2026);

        // Then
        assertThat(result.athleteName()).isEqualTo("Mario Runner");
        assertThat(result.currentGoal()).isEqualTo("Run sub-4h marathon");
    }

    @Test
    void shouldThrowWhenReportNotFound() {
        // Given
        String userId = "mario-runner";
        when(repository.findByUserIdAndWeekNumberAndYear(userId, 99, 2026)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getReportByUserIdAndWeek(userId, 99, 2026))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Report not found");
    }
}
