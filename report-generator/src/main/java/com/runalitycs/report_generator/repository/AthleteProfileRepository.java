package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.AthleteProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AthleteProfileRepository extends JpaRepository<AthleteProfile, UUID> {
    Optional<AthleteProfile> findByUserId(String userId);
}
