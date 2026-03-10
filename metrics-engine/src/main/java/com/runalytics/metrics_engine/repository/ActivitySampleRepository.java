package com.runalytics.metrics_engine.repository;

import com.runalytics.metrics_engine.entity.ActivitySample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ActivitySampleRepository extends JpaRepository<ActivitySample, UUID> {

    List<ActivitySample> findByActivityIdOrderByTimestampAsc(UUID activityId);
}