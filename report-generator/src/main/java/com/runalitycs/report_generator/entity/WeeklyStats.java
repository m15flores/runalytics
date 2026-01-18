package com.runalitycs.report_generator.entity;


import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeeklyStats {

    private Integer weekNumber;
    private Integer year;

    // Basic aggregations
    private Integer totalActivities;
    private BigDecimal totalDistance; // km
    private Integer totalDuration; // seconds
    private Integer totalCalories;

    // Weighted averages
    private Integer averagePace; // seconds per km
    private Integer averageHeartRate; // bpm
    private Integer averageCadence; // spm

    // Elevation
    private Integer totalAscent; // meters
    private Integer totalDescent; // meters

    // Heart rate zones distribution (total seconds)
    private Map<String, Integer> hrZonesDistribution;

    // Training load
    private Double totalTrainingEffect;
    private Double totalAnaerobicEffect;

    // Trends
    private Double distanceChangePercent; // % change from previous week
    private String trend; // "improving", "stable", "declining"
}