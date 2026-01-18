package com.runalitycs.report_generator.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;


@Entity
@Table(name = "activity_metrics")
@Immutable // Read-only
@Getter
@Setter
public class ActivityMetrics {

    @Id
    private UUID id;

    @Column(name = "activity_id")
    private UUID activityId;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "total_distance")
    private BigDecimal totalDistance;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Column(name = "total_calories")
    private Integer totalCalories;

    @Column(name = "average_pace")
    private Integer averagePace;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "average_cadence")
    private Integer averageCadence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hr_zones")
    private Map<String, Integer> hrZones;

    @Column(name = "total_ascent")
    private Integer totalAscent;

    @Column(name = "total_descent")
    private Integer totalDescent;

    @Column(name = "training_effect")
    private Double trainingEffect;

    @Column(name = "anaerobic_training_effect")
    private Double anaerobicTrainingEffect;

    @Column(name = "calculated_at")
    private Instant calculatedAt;
}