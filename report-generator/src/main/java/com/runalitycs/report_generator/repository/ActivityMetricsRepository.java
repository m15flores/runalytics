package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.ActivityMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ActivityMetricsRepository extends JpaRepository<ActivityMetrics, UUID> {

    @Query("SELECT am FROM ActivityMetrics am WHERE am.userId = :userId " +
            "AND am.startedAt BETWEEN :startDate AND :endDate " +
            "ORDER BY am.startedAt DESC")
    List<ActivityMetrics> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
}