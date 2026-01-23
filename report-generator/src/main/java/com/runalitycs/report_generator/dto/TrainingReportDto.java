package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

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
        UUID triggerActivityId
){}
