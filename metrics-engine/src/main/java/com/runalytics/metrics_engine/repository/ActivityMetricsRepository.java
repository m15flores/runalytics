package com.runalytics.metrics_engine.repository;

import com.runalytics.metrics_engine.entity.ActivityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, UUID> {
    Optional<ActivityMetrics> findByActivityId(UUID activityId);
    boolean existsByActivityId(UUID activityId);
}
