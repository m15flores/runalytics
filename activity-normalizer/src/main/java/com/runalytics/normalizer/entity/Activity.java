package com.runalytics.normalizer.entity;

import com.runalytics.normalizer.dto.ActivitySample;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "activities")
@Getter
@Setter
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "device", length = 100)
    private String device;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "distance_meters", precision = 10, scale = 2)
    private BigDecimal distanceMeters;

    @Column(name = "samples", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ActivitySample> samples;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}