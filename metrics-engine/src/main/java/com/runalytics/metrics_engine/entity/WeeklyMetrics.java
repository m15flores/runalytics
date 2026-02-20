package com.runalytics.metrics_engine.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "weekly_metrics")
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

    // Agregados semanales
    @Column(name = "total_activities")
    private Integer totalActivities;

    @Column(name = "total_distance")
    private BigDecimal totalDistance;

    @Column(name = "total_duration")
    private Integer totalDuration;

    @Column(name = "total_calories")
    private Integer totalCalories;

    // Promedios semanales
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

    public WeeklyMetrics() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public LocalDate getWeekStartDate() {
        return weekStartDate;
    }

    public void setWeekStartDate(LocalDate weekStartDate) {
        this.weekStartDate = weekStartDate;
    }

    public LocalDate getWeekEndDate() {
        return weekEndDate;
    }

    public void setWeekEndDate(LocalDate weekEndDate) {
        this.weekEndDate = weekEndDate;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getTotalActivities() {
        return totalActivities;
    }

    public void setTotalActivities(Integer totalActivities) {
        this.totalActivities = totalActivities;
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

    public Integer getAverageHeartRate() {
        return averageHeartRate;
    }

    public void setAverageHeartRate(Integer averageHeartRate) {
        this.averageHeartRate = averageHeartRate;
    }

    public Integer getAverageCadence() {
        return averageCadence;
    }

    public void setAverageCadence(Integer averageCadence) {
        this.averageCadence = averageCadence;
    }

    public Double getWeeklyTrainingLoad() {
        return weeklyTrainingLoad;
    }

    public void setWeeklyTrainingLoad(Double weeklyTrainingLoad) {
        this.weeklyTrainingLoad = weeklyTrainingLoad;
    }

    public BigDecimal getWeeklyVolume() {
        return weeklyVolume;
    }

    public void setWeeklyVolume(BigDecimal weeklyVolume) {
        this.weeklyVolume = weeklyVolume;
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