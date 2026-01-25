package com.runalytics.ai_coach.repository;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, UUID> {
    List<Recommendation> findByUserId(String userId);
    List<Recommendation> findByReportId(UUID reportId);
    List<Recommendation> findByUserIdAndPriority(String userId, Priority priority);
    /**
     * Find active (non-expired) recommendations for a user
     * A recommendation is active if:
     * - expiresAt is null (never expires), OR
     * - expiresAt is after the given instant
     */
    @Query("SELECT r FROM Recommendation r WHERE r.userId = :userId " +
            "AND (r.expiresAt IS NULL OR r.expiresAt > :now)")
    List<Recommendation> findActiveRecommendationsByUserId(
            @Param("userId") String userId,
            @Param("now") Instant now
    );

}
