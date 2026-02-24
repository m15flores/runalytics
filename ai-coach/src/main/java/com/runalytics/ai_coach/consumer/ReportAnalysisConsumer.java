package com.runalytics.ai_coach.consumer;

import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import com.runalytics.ai_coach.dto.TrainingReportEventDto;
import com.runalytics.ai_coach.producer.RecommendationGeneratedProducer;
import com.runalytics.ai_coach.service.RecommendationGeneratorService;
import com.runalytics.ai_coach.service.TrainingCycleContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Kafka consumer for training report events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportAnalysisConsumer {

    private final RecommendationGeneratorService recommendationGeneratorService;
    private final TrainingCycleContextService trainingCycleContextService;
    private final RecommendationGeneratedProducer recommendationProducer;
    private final KafkaTemplate<String, TrainingReportEventDto> dlqTemplate;

    @Value("${app.kafka.topics.dlq}")
    private String dlqTopic;

    /**
     * Consume training report events and generate AI recommendations
     */
    @KafkaListener(
            topics = "${app.kafka.topics.reports-generated}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(TrainingReportEventDto event) {
        log.info("Received training report event: reportId={}, userId={}, week={}/{}",
                event.getReportId(), event.getUserId(), event.getWeekNumber(), event.getYear());

        try {
            // Step 1: Determine training cycle context
            TrainingCycleContext cycleContext = trainingCycleContextService
                    .determineContext(event.getUserId(), event.getWeekNumber());

            log.debug("Determined cycle context: weekInCycle={}, phase={}, deload={}",
                    cycleContext.getWeekInCycle(),
                    cycleContext.getPhase(),
                    cycleContext.getIsDeloadWeek());

            // Step 2: Convert event to DTO
            TrainingReportDto report = TrainingReportDto.builder()
                    .id(event.getReportId())
                    .userId(event.getUserId())
                    .weekNumber(event.getWeekNumber())
                    .year(event.getYear())
                    .summaryJson(event.getSummaryJson())
                    .athleteName(event.getAthleteName())
                    .currentGoal(event.getCurrentGoal())
                    .createdAt(event.getGeneratedAt())
                    .triggerActivityId(event.getTriggerActivityId())
                    .build();

            // Step 3: Generate recommendations
            List<Recommendation> recommendations = recommendationGeneratorService
                    .generateRecommendations(report, cycleContext);

            log.info("Generated {} recommendations for report: {}",
                    recommendations.size(), event.getReportId());

            // Step 4: Publish recommendations event
            recommendationProducer.publishRecommendations(event, recommendations);

        } catch (Exception e) {
            log.error("Error processing training report event: reportId={}, error={}",
                    event.getReportId(), e.getMessage(), e);
            dlqTemplate.send(dlqTopic, event.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to send event to DLQ: reportId={}, error={}",
                                    event.getReportId(), ex.getMessage());
                        } else {
                            log.warn("Sent failed event to DLQ: reportId={}, topic={}",
                                    event.getReportId(), dlqTopic);
                        }
                    });
        }
    }
}