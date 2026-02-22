package com.runalytics.metrics_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "activity_metrics",
        uniqueConstraints = @UniqueConstraint(columnNames = "activity_id"))
@Getter
@Setter
public class ActivityMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    // Basic metrics
    @Column(name = "total_distance", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDistance;

    @Column(name = "total_duration", nullable = false)
    private Integer totalDuration;

    @Column(name = "total_elapsed_time")
    private Integer totalElapsedTime;

    @Column(name = "total_calories")
    private Integer totalCalories;

    // Pace & Speed (calculated)
    @Column(name = "average_pace")
    private Integer averagePace;

    @Column(name = "max_pace")
    private Integer maxPace;

    @Column(name = "average_speed", precision = 6, scale = 3)
    private BigDecimal averageSpeed;

    @Column(name = "max_speed", precision = 6, scale = 3)
    private BigDecimal maxSpeed;

    @Column(name = "average_gap")
    private Integer averageGAP;

    // Heart Rate
    @Column(name = "average_heart_rate")
    private Integer averageHeartRate;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "min_heart_rate")
    private Integer minHeartRate;

    @Column(name = "hr_zones", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> hrZones;

    @Column(name = "hr_zones_percentage", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> hrZonesPercentage;

    // Cadence
    @Column(name = "average_cadence")
    private Integer averageCadence;

    @Column(name = "max_cadence")
    private Integer maxCadence;

    // Running Dynamics
    @Column(name = "average_vertical_oscillation")
    private Double averageVerticalOscillation;

    @Column(name = "average_stance_time")
    private Double averageStanceTime;

    @Column(name = "average_vertical_ratio")
    private Double averageVerticalRatio;

    @Column(name = "average_step_length")
    private Integer averageStepLength;

    // Power
    @Column(name = "average_power")
    private Integer averagePower;

    @Column(name = "max_power")
    private Integer maxPower;

    @Column(name = "normalized_power")
    private Integer normalizedPower;

    @Column(name = "power_zones", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Integer> powerZones;

    // Elevation
    @Column(name = "total_ascent")
    private Integer totalAscent;

    @Column(name = "total_descent")
    private Integer totalDescent;

    // Training Load
    @Column(name = "training_effect")
    private Double trainingEffect;

    @Column(name = "anaerobic_training_effect")
    private Double anaerobicTrainingEffect;

    @Column(name = "training_load_peak")
    private Double trainingLoadPeak;

    // Subjective
    @Column(name = "workout_feel")
    private Integer workoutFeel;

    @Column(name = "workout_rpe")
    private Integer workoutRpe;

    // Metadata
    @Column(name = "calculated_at", nullable = false)
    private Instant calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (calculatedAt == null) {
            calculatedAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}