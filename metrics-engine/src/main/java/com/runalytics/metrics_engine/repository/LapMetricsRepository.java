package com.runalytics.metrics_engine.repository;

import com.runalytics.metrics_engine.entity.LapMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LapMetricsRepository extends JpaRepository<LapMetrics, UUID> {
    List<LapMetrics> findByActivityIdOrderByLapNumberAsc(UUID activityId);
    void deleteByActivityId(UUID activityId);

}
