package com.runalytics.metrics_engine.repository;

import com.runalytics.metrics_engine.entity.WeeklyMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WeeklyMetricsRepository extends JpaRepository<WeeklyMetrics, UUID> {
    Optional<WeeklyMetrics> findByUserIdAndWeekStartDate(String userId, LocalDate weekStartDate);
    List<WeeklyMetrics> findByUserIdOrderByWeekStartDateDesc(String userId);
    List<WeeklyMetrics> findByUserIdAndYearOrderByWeekNumberAsc(String userId, Integer year);
}
