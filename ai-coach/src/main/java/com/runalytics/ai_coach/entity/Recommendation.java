package com.runalytics.ai_coach.entity;

import com.runalytics.ai_coach.dto.TrainingCycleContext;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recommendations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID reportId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Builder.Default
    private Boolean applied = false;

    @Enumerated(EnumType.STRING)
    private TrainingVerdict verdict;

    private Integer weekInCycle;

    @Enumerated(EnumType.STRING)
    private TrainingCycleContext.TrainingPhase trainingPhase;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
