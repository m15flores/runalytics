package com.runalytics.metrics_engine.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "lap_metrics")
public class LapMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // FK a activity_metrics
    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "lap_number", nullable = false)
    private Integer lapNumber;

    @Column(name = "lap_name", length = 100)
    private String lapName;

    @Column(name = "intensity", length = 20)
    private String intensity;

    @Column(name = "start_time")
    private Instant startTime;

    // Basic metrics
    @Column(name = "distance")
    private BigDecimal distance;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "calories")
    private Integer calories;

    // Pace & Speed
    @Column(name = "pace")
    private Integer pace;

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

    // Elevation
    @Column(name = "total_ascent")
    private Integer totalAscent;

    @Column(name = "total_descent")
    private Integer totalDescent;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public LapMetrics() {}

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

    public Integer getLapNumber() {
        return lapNumber;
    }

    public void setLapNumber(Integer lapNumber) {
        this.lapNumber = lapNumber;
    }

    public String getLapName() {
        return lapName;
    }

    public void setLapName(String lapName) {
        this.lapName = lapName;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getDistance() {
        return distance;
    }

    public void setDistance(BigDecimal distance) {
        this.distance = distance;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getCalories() {
        return calories;
    }

    public void setCalories(Integer calories) {
        this.calories = calories;
    }

    public Integer getPace() {
        return pace;
    }

    public void setPace(Integer pace) {
        this.pace = pace;
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
