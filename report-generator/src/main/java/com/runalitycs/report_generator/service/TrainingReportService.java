package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.entity.TrainingReport;
import com.runalitycs.report_generator.repository.AthleteProfileRepository;
import com.runalitycs.report_generator.repository.TrainingReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrainingReportService {

    private final TrainingReportRepository repository;
    private final AthleteProfileRepository athleteProfileRepository;

    public List<TrainingReportDto> getReportsByUserId(String userId) {
        log.info("action=getReports userId={}", userId);
        AthleteProfile profile = athleteProfileRepository.findByUserId(userId).orElse(null);
        return repository.findByUserIdOrderByYearDescWeekNumberDesc(userId).stream()
                .map(report -> toDto(report, profile))
                .toList();
    }

    public TrainingReportDto getReportByUserIdAndWeek(String userId, int weekNumber, int year) {
        log.info("action=getReport userId={} weekNumber={} year={}", userId, weekNumber, year);
        TrainingReport report = repository.findByUserIdAndWeekNumberAndYear(userId, weekNumber, year)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Report not found for userId: " + userId + ", week: " + weekNumber + ", year: " + year));
        AthleteProfile profile = athleteProfileRepository.findByUserId(userId).orElse(null);
        return toDto(report, profile);
    }

    private TrainingReportDto toDto(TrainingReport report, AthleteProfile profile) {
        return TrainingReportDto.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .weekNumber(report.getWeekNumber())
                .year(report.getYear())
                .markdownContent(report.getMarkdownContent())
                .summaryJson(report.getSummaryJson())
                .createdAt(report.getCreatedAt())
                .triggerActivityId(report.getTriggerActivityId())
                .athleteName(profile != null ? profile.getName() : null)
                .currentGoal(profile != null ? profile.getCurrentGoal() : null)
                .build();
    }
}