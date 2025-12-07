package com.runalitycs.metrics_engine.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "activity_metrics")
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

    // Constructors
    public ActivityMetrics() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getActivityId() {
        return activityId;
    }

    public void setActivityId(UUID activityId) {
        this.activityId = activityId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(BigDecimal totalDistance) {
        this.totalDistance = totalDistance;
    }

    public Integer getTotalDuration() {
        return totalDuration;
    }

    public void setTotalDuration(Integer totalDuration) {
        this.totalDuration = totalDuration;
    }

    public Integer getTotalElapsedTime() {
        return totalElapsedTime;
    }

    public void setTotalElapsedTime(Integer totalElapsedTime) {
        this.totalElapsedTime = totalElapsedTime;
    }

    public Integer getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(Integer totalCalories) {
        this.totalCalories = totalCalories;
    }

    public Integer getAveragePace() {
        return averagePace;
    }

    public void setAveragePace(Integer averagePace) {
        this.averagePace = averagePace;
    }

    public Integer getMaxPace() {
        return maxPace;
    }

    public void setMaxPace(Integer maxPace) {
        this.maxPace = maxPace;
    }

    public BigDecimal getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(BigDecimal averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

    public BigDecimal getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(BigDecimal maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Integer getAverageGAP() {
        return averageGAP;
    }

    public void setAverageGAP(Integer averageGAP) {
        this.averageGAP = averageGAP;
    }

    public Integer getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(Integer averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public Integer getMaxHeartRate() {
        return maxHeartRate;
    }

    public void setMaxHeartRate(Integer maxHeartRate) {
        this.maxHeartRate = maxHeartRate;
    }

    public Integer getMinHeartRate() {
        return minHeartRate;
    }

    public void setMinHeartRate(Integer minHeartRate) {
        this.minHeartRate = minHeartRate;
    }

    public Map<String, Integer> getHrZones() {
        return hrZones;
    }

    public void setHrZones(Map<String, Integer> hrZones) {
        this.hrZones = hrZones;
    }

    public Map<String, Integer> getHrZonesPercentage() {
        return hrZonesPercentage;
    }

    public void setHrZonesPercentage(Map<String, Integer> hrZonesPercentage) {
        this.hrZonesPercentage = hrZonesPercentage;
    }

    public Integer getAverageCadence() {
        return averageCadence;
    }

    public void setAverageCadence(Integer averageCadence) {
        this.averageCadence = averageCadence;
    }

    public Integer getMaxCadence() {
        return maxCadence;
    }

    public void setMaxCadence(Integer maxCadence) {
        this.maxCadence = maxCadence;
    }

    public Double getAverageVerticalOscillation() {
        return averageVerticalOscillation;
    }

    public void setAverageVerticalOscillation(Double averageVerticalOscillation) {
        this.averageVerticalOscillation = averageVerticalOscillation;
    }

    public Double getAverageStanceTime() {
        return averageStanceTime;
    }

    public void setAverageStanceTime(Double averageStanceTime) {
        this.averageStanceTime = averageStanceTime;
    }

    public Double getAverageVerticalRatio() {
        return averageVerticalRatio;
    }

    public void setAverageVerticalRatio(Double averageVerticalRatio) {
        this.averageVerticalRatio = averageVerticalRatio;
    }

    public Integer getAverageStepLength() {
        return averageStepLength;
    }

    public void setAverageStepLength(Integer averageStepLength) {
        this.averageStepLength = averageStepLength;
    }

    public Integer getAveragePower() {
        return averagePower;
    }

    public void setAveragePower(Integer averagePower) {
        this.averagePower = averagePower;
    }

    public Integer getMaxPower() {
        return maxPower;
    }

    public void setMaxPower(Integer maxPower) {
        this.maxPower = maxPower;
    }

    public Integer getNormalizedPower() {
        return normalizedPower;
    }

    public void setNormalizedPower(Integer normalizedPower) {
        this.normalizedPower = normalizedPower;
    }

    public Map<String, Integer> getPowerZones() {
        return powerZones;
    }

    public void setPowerZones(Map<String, Integer> powerZones) {
        this.powerZones = powerZones;
    }

    public Integer getTotalAscent() {
        return totalAscent;
    }

    public void setTotalAscent(Integer totalAscent) {
        this.totalAscent = totalAscent;
    }

    public Integer getTotalDescent() {
        return totalDescent;
    }

    public void setTotalDescent(Integer totalDescent) {
        this.totalDescent = totalDescent;
    }

    public Double getTrainingEffect() {
        return trainingEffect;
    }

    public void setTrainingEffect(Double trainingEffect) {
        this.trainingEffect = trainingEffect;
    }

    public Double getAnaerobicTrainingEffect() {
        return anaerobicTrainingEffect;
    }

    public void setAnaerobicTrainingEffect(Double anaerobicTrainingEffect) {
        this.anaerobicTrainingEffect = anaerobicTrainingEffect;
    }

    public Double getTrainingLoadPeak() {
        return trainingLoadPeak;
    }

    public void setTrainingLoadPeak(Double trainingLoadPeak) {
        this.trainingLoadPeak = trainingLoadPeak;
    }

    public Integer getWorkoutFeel() {
        return workoutFeel;
    }

    public void setWorkoutFeel(Integer workoutFeel) {
        this.workoutFeel = workoutFeel;
    }

    public Integer getWorkoutRpe() {
        return workoutRpe;
    }

    public void setWorkoutRpe(Integer workoutRpe) {
        this.workoutRpe = workoutRpe;
    }

    public Instant getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(Instant calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}