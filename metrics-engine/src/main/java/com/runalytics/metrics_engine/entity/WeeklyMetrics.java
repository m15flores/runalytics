package com.runalytics.metrics_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// NOTE: This entity is reserved for temporal-aggregator service.
// metrics-engine defines the schema but does not write to weekly_metrics.
@Entity
@Table(name = "weekly_metrics")
@Getter
@Setter
public class WeeklyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "week_end_date", nullable = false)
    private LocalDate weekEndDate;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    // Weekly aggregates
    @Column(name = "total_activities")
    private Integer totalActivities;

    @Column(name = "total_distance")
    private BigDecimal totalDistance;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Column(name = "total_calories")
    private Integer totalCalories;

    // Weekly averages
    @Column(name = "average_pace")
    private Integer averagePace;

    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "average_cadence")
    private Integer averageCadence;

    // Training Load
    @Column(name = "weekly_training_load")
    private Double weeklyTrainingLoad;

    @Column(name = "weekly_volume")
    private BigDecimal weeklyVolume;

    // Metadata
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}