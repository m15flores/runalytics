package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.repository.AthleteProfileRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AthleteProfileService {

    private final AthleteProfileRepository athleteProfileRepository;

    @Transactional
    public AthleteProfile createProfile(AthleteProfile profile) {
        log.info("Creating athlete profile for userId: {}", profile.getUserId());

        if (athleteProfileRepository.existsByUserId(profile.getUserId())) {
            throw new IllegalArgumentException(
                    "Profile already exists for userId: " + profile.getUserId()
            );
        }

        AthleteProfile saved = athleteProfileRepository.save(profile);
        log.info("Created athlete profile with id: {}", saved.getId());

        return saved;
    }

    @Transactional()
    public AthleteProfile getProfileByUserId(String userId) {
        log.debug("Fetching profile for userId: {}", userId);

        return athleteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for userId: " + userId
                ));
    }

    @Transactional
    public AthleteProfile updateProfile(String userId, AthleteProfile updatedProfile) {
        log.info("Updating profile for userId: {}", userId);

        AthleteProfile existing = athleteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for userId: " + userId
                ));

        // Update fields
        existing.setName(updatedProfile.getName());
        existing.setAge(updatedProfile.getAge());
        existing.setWeight(updatedProfile.getWeight());
        existing.setMaxHeartRate(updatedProfile.getMaxHeartRate());

        AthleteProfile saved = athleteProfileRepository.save(existing);
        log.info("Updated profile for userId: {}", userId);

        return saved;
    }

    @Transactional
    public void deleteProfile(String userId) {
        log.info("Deleting profile for userId: {}", userId);

        AthleteProfile profile = athleteProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Profile not found for userId: " + userId
                ));

        athleteProfileRepository.delete(profile);
        log.info("Deleted profile for userId: {}", userId);
    }

    @Transactional(readOnly = true)
    public boolean profileExists(String userId) {
        return athleteProfileRepository.existsByUserId(userId);
    }
}
