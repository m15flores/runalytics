package com.runalytics.metrics_engine.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_samples")
@Getter
@Setter
public class ActivitySample {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "activity_id", nullable = false)
    private UUID activityId;

    @Column(name = "sample_timestamp")
    private Instant timestamp;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "cadence")
    private Integer cadence;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "power")
    private Integer power;

    @Column(name = "distance")
    private Double distance;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}