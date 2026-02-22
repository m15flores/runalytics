package com.runalitycs.report_generator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "athlete_profile")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AthleteProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "name")
    private String name;

    @Column(name = "age")
    private Integer age;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "max_heart_rate")
    private Integer maxHeartRate;

    @Column(name = "current_goal")
    private String currentGoal;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}