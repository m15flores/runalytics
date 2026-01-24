package com.runalitycs.report_generator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.dto.ActivityMetricsDto;
import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.entity.TrainingReport;
import com.runalitycs.report_generator.mapper.TrainingReportMapper;
import com.runalitycs.report_generator.repository.TrainingReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGeneratorService {

    private final AthleteProfileService athleteProfileService;
    private final WeeklyAggregationService weeklyAggregationService;
    private final MarkdownTemplateService markdownTemplateService;
    private final TrainingReportRepository trainingReportRepository;
    private final ObjectMapper objectMapper;
    private final TrainingReportMapper trainingReportMapper;

    private static final int WEEKS_TO_AGGREGATE = 4;
    private static final String DEFAULT_TIMEZONE = "Europe/Paris";

    /**
     * Generate training report for an activity
     */
    @Transactional
    public TrainingReportDto generateReport(ActivityMetricsDto activityMetrics) {
        log.info("Generating report for userId: {}, activityId: {}",
                activityMetrics.userId(), activityMetrics.activityId());

        // 1. Get athlete profile
        AthleteProfile profile = athleteProfileService.getProfileByUserId(activityMetrics.userId());

        // 2. Determine week number and year
        ZoneId zoneId = ZoneId.of(DEFAULT_TIMEZONE);
        WeekFields weekFields = WeekFields.of(DayOfWeek.MONDAY, 4);

        int weekNumber = activityMetrics.startedAt()
                .atZone(zoneId)
                .toLocalDate()
                .get(weekFields.weekOfWeekBasedYear());

        int year = activityMetrics.startedAt()
                .atZone(zoneId)
                .toLocalDate()
                .get(weekFields.weekBasedYear());

        log.debug("Activity belongs to week {}/{}", weekNumber, year);

        // 3. Check if report already exists for this week
        Optional<TrainingReport> existingReport = trainingReportRepository
                .findByUserIdAndWeekNumberAndYear(activityMetrics.userId(), weekNumber, year);

        if (existingReport.isPresent()) {
            log.info("Report already exists for week {}/{}. Regenerating.", weekNumber, year);
            trainingReportRepository.delete(existingReport.get());
        }

        // 4. Get weekly stats (last 4 weeks)
        List<WeeklyStats> weeklyStats = weeklyAggregationService.getWeeklyStats(
                activityMetrics.userId(),
                WEEKS_TO_AGGREGATE
        );

        if (weeklyStats.isEmpty()) {
            throw new IllegalStateException("No weekly stats available for userId: " + activityMetrics.userId());
        }

        WeeklyStats currentWeek = weeklyStats.get(0); // Most recent week

        // 5. Generate markdown content
        String markdownContent = markdownTemplateService.generateWeeklyReport(
                profile,
                currentWeek,
                weeklyStats
        );

        // 6. Create summary JSON
        String summaryJson = createSummaryJson(currentWeek);

        // 7. Find existing report or create new one
        TrainingReport report = trainingReportRepository
                .findByUserIdAndWeekNumberAndYear(activityMetrics.userId(), weekNumber, year)
                .orElse(TrainingReport.builder()
                        .userId(activityMetrics.userId())
                        .weekNumber(weekNumber)
                        .year(year)
                        .build());

        // 8. Update report fields
        report.setMarkdownContent(markdownContent);
        report.setSummaryJson(summaryJson);
        report.setTriggerActivityId(activityMetrics.activityId());

        TrainingReport saved = trainingReportRepository.save(report);

        if (report.getId() == null) {
            log.info("Generated new report with id: {} for week {}/{}", saved.getId(), weekNumber, year);
        } else {
            log.info("Updated existing report with id: {} for week {}/{}", saved.getId(), weekNumber, year);
        }

        return trainingReportMapper.toDto(saved);
    }

    private String createSummaryJson(WeeklyStats weekStats) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("weekNumber", weekStats.getWeekNumber());
        summary.put("year", weekStats.getYear());
        summary.put("totalActivities", weekStats.getTotalActivities());
        summary.put("totalKm", weekStats.getTotalDistance());
        summary.put("totalDuration", weekStats.getTotalDuration());

        if (weekStats.getAveragePace() != null) {
            summary.put("averagePace", weekStats.getAveragePace());
        }

        if (weekStats.getTrend() != null) {
            summary.put("trend", weekStats.getTrend());
        }

        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            log.error("Error creating summary JSON", e);
            return "{}";
        }
    }
}
