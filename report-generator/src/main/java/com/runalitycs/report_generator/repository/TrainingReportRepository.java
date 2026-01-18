package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.TrainingReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TrainingReportRepository extends JpaRepository<TrainingReport, UUID> {
    Optional<TrainingReport> findByUserIdAndWeekNumberAndYear(String userId, Integer weekNumber, Integer year);
    List<TrainingReport> findByUserIdOrderByYearDescWeekNumberDesc(String userId);
}
