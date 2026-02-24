package com.runalytics.ai_coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event received from Kafka (reports.generated topic)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingReportEventDto {

    private UUID reportId;
    private String userId;
    private Integer weekNumber;
    private Integer year;
    private String summaryJson;
    private String athleteName;
    private String currentGoal;
    private Instant generatedAt;
    private UUID triggerActivityId;
}
