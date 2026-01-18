package com.runalitycs.report_generator.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "training_reports",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"user_id", "week_number", "year"}
        )
)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TrainingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "trigger_activity_id")
    private UUID triggerActivityId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "markdown_content", nullable = false, columnDefinition = "TEXT")
    private String markdownContent;

    @Column(name = "summary_json", columnDefinition = "TEXT")
    private String summaryJson;

    // Metadata
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
