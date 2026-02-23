package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Builder
public record ReportGeneratedEventDto (
        @JsonProperty("reportId")
        UUID reportId,

        @JsonProperty("userId")
        String userId,

        @JsonProperty("weekNumber")
        Integer weekNumber,

        @JsonProperty("year")
        Integer year,

        @JsonProperty("summaryJson")
        String summaryJson,

        @JsonProperty("generatedAt")
        Instant generatedAt,

        @JsonProperty("triggerActivityId")
        UUID triggerActivityId,

        @JsonProperty("athleteName")
        String athleteName,

        @JsonProperty("currentGoal")
        String currentGoal
) {}
