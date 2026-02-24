package com.runalytics.ai_coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Full training report DTO (for prompt generation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingReportDto {
    private UUID id;
    private String userId;
    private Integer weekNumber;
    private Integer year;
    private String athleteName;
    private String currentGoal;
    private String markdownContent;
    private String summaryJson;
    private Instant createdAt;
    private UUID triggerActivityId;
}