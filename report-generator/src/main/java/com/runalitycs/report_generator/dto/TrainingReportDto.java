package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record TrainingReportDto (
        @JsonProperty("id")
        UUID id,

        @JsonProperty("userId")
        String userId,

        @JsonProperty("weekNumber")
        Integer weekNumber,

        @JsonProperty("year")
        Integer year,

        @JsonProperty("markdownContent")
        String markdownContent,

        @JsonProperty("summaryJson")
        String summaryJson,

        @JsonProperty("createdAt")
        Instant createdAt,

        @JsonProperty("triggerActivityId")
        UUID triggerActivityId,

        @JsonProperty("athleteName")
        String athleteName,

        @JsonProperty("currentGoal")
        String currentGoal
){}
