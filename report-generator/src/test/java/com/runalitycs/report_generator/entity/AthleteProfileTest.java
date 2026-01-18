package com.runalitycs.report_generator.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AthleteProfileTest {

    @Test
    void shouldCreateAthleteProfileWithRequiredFields() {
        // Given
        AthleteProfile profile = new AthleteProfile();
        profile.setUserId("user-12345");
        profile.setName("Test Runner");

        // When & Then
        assertNotNull(profile);
        assertEquals("user-12345", profile.getUserId());
        assertEquals("Test Runner", profile.getName());
        assertNull(profile.getId()); // Not persisted yet
    }

    @Test
    void shouldCreateAthleteProfileWithBuilder() {
        // Given & When
        AthleteProfile profile = AthleteProfile.builder()
                .userId("mario-runner")
                .name("Mario")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();

        // Then
        assertEquals("mario-runner", profile.getUserId());
        assertEquals("Mario", profile.getName());
        assertEquals(30, profile.getAge());
        assertEquals(70.0, profile.getWeight());
        assertEquals(190, profile.getMaxHeartRate());
    }

    @Test
    void shouldSetOptionalFields() {
        // Given
        AthleteProfile profile = new AthleteProfile();
        profile.setUserId("user-12345");
        profile.setName("Test Runner");
        profile.setAge(35);
        profile.setWeight(75.5);
        profile.setMaxHeartRate(185);

        // Then
        assertEquals(35, profile.getAge());
        assertEquals(75.5, profile.getWeight());
        assertEquals(185, profile.getMaxHeartRate());
    }

    @Test
    void shouldInitializeTimestampsOnPrePersist() {
        // Given
        AthleteProfile profile = new AthleteProfile();
        profile.setUserId("user-12345");
        profile.setName("Test Runner");

        // When
        profile.onCreate(); // Simulate @PrePersist

        // Then
        assertNotNull(profile.getCreatedAt());
        assertNotNull(profile.getUpdatedAt());
    }

    @Test
    void shouldUpdateTimestampOnPreUpdate() throws InterruptedException {
        // Given
        AthleteProfile profile = new AthleteProfile();
        profile.setUserId("user-12345");
        profile.setName("Test Runner");
        profile.onCreate();

        Instant originalCreatedAt = profile.getCreatedAt();
        Instant originalUpdatedAt = profile.getUpdatedAt();

        // Wait to ensure timestamp difference
        Thread.sleep(10);

        // When
        profile.onUpdate(); // Simulate @PreUpdate

        // Then
        assertEquals(originalCreatedAt, profile.getCreatedAt()); // Should not change
        assertNotEquals(originalUpdatedAt, profile.getUpdatedAt());
        assertTrue(profile.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void shouldAllowNullOptionalFields() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("minimal-user")
                .name("Minimal Profile")
                .build();

        // Then
        assertNotNull(profile.getUserId());
        assertNotNull(profile.getName());
        assertNull(profile.getAge());
        assertNull(profile.getWeight());
        assertNull(profile.getMaxHeartRate());
    }
}