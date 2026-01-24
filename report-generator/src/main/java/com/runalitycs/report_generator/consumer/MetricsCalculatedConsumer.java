package com.runalitycs.report_generator.consumer;

import com.runalitycs.report_generator.dto.ActivityMetricsDto;
import com.runalitycs.report_generator.dto.ReportGeneratedEventDto;
import com.runalitycs.report_generator.dto.TrainingReportDto;
import com.runalitycs.report_generator.producer.ReportGeneratedProducer;
import com.runalitycs.report_generator.service.ReportGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsCalculatedConsumer {

    private final ReportGeneratorService reportGeneratorService;
    private final ReportGeneratedProducer reportGeneratedProducer;

    @KafkaListener(
            topics = "${app.kafka.topics.metrics-calculated}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(ActivityMetricsDto activityMetrics) {
        log.info("Received metrics calculated event for activityId: {}, userId: {}",
                activityMetrics.activityId(), activityMetrics.userId());

        try {
            // Generate report
            TrainingReportDto report = reportGeneratorService.generateReport(activityMetrics);

            log.info("Generated report id: {} for week {}/{}",
                    report.id(), report.weekNumber(), report.year());

            // Publish report generated event
            ReportGeneratedEventDto event = ReportGeneratedEventDto.builder()
                    .reportId(report.id())
                    .userId(report.userId())
                    .weekNumber(report.weekNumber())
                    .year(report.year())
                    .summaryJson(report.summaryJson())
                    .generatedAt(report.createdAt())
                    .triggerActivityId(report.triggerActivityId())
                    .build();

            reportGeneratedProducer.publish(event);

        } catch (Exception e) {
            log.error("Error processing metrics calculated event for activityId: {}",
                    activityMetrics.activityId(), e);
            throw e; // Let Kafka retry mechanism handle it
        }
    }
}
