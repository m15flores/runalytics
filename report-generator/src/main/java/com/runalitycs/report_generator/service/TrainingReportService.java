package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.entity.TrainingReport;
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

    public List<TrainingReport> getReportsByUserId(String userId) {
        log.info("action=getReports userId={}", userId);
        return repository.findByUserIdOrderByYearDescWeekNumberDesc(userId);
    }

    public TrainingReport getReportByUserIdAndWeek(String userId, int weekNumber, int year) {
        log.info("action=getReport userId={} weekNumber={} year={}", userId, weekNumber, year);
        return repository.findByUserIdAndWeekNumberAndYear(userId, weekNumber, year)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Report not found for userId: " + userId + ", week: " + weekNumber + ", year: " + year));
    }
}