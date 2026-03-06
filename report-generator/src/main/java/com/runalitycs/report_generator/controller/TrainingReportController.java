package com.runalitycs.report_generator.controller;

import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.entity.TrainingReport;
import com.runalitycs.report_generator.mapper.TrainingReportMapper;
import com.runalitycs.report_generator.service.TrainingReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class TrainingReportController {

    private final TrainingReportService trainingReportService;
    private final TrainingReportMapper mapper;

    @GetMapping("/users/{userId}")
    public ResponseEntity<List<TrainingReportDto>> getReportsByUserId(@PathVariable String userId) {
        log.info("GET /api/reports/users/{} - Fetching reports", userId);

        List<TrainingReportDto> dtos = trainingReportService.getReportsByUserId(userId).stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/users/{userId}/{weekNumber}/{year}")
    public ResponseEntity<TrainingReportDto> getReportByUserIdAndWeek(
            @PathVariable String userId,
            @PathVariable int weekNumber,
            @PathVariable int year) {

        log.info("GET /api/reports/users/{}/{}/{} - Fetching report", userId, weekNumber, year);

        TrainingReport report = trainingReportService.getReportByUserIdAndWeek(userId, weekNumber, year);

        return ResponseEntity.ok(mapper.toDto(report));
    }

}