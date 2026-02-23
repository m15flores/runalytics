package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.AthleteProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class AthleteProfileRepositoryTest {

    private static final Instant NOW = Instant.parse("2024-12-10T12:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AthleteProfileRepository repository;

    @Test
    void shouldSaveAthleteProfile() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("test-user-1")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        // When
        AthleteProfile saved = repository.save(profile);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("test-user-1");
        assertThat(saved.getName()).isEqualTo("Test Runner");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldFindAthleteProfileByUserId() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("test-runner")
                .name("Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        repository.save(profile);

        // When
        Optional<AthleteProfile> found = repository.findByUserId("test-runner");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("test-runner");
        assertThat(found.get().getName()).isEqualTo("Runner");
    }

    @Test
    void shouldNotAllowDuplicateUserId() {
        // Given
        AthleteProfile profile1 = AthleteProfile.builder()
                .userId("duplicate-user")
                .name("User One")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        AthleteProfile profile2 = AthleteProfile.builder()
                .userId("duplicate-user")
                .name("User Two")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        repository.save(profile1);

        // When & Then
        assertThatThrownBy(() -> {
            repository.save(profile2);
            repository.flush(); // Force db to write
        }).isInstanceOf(Exception.class); // Could be DataIntegrityViolationException
    }

    @Test
    void shouldUpdateAthleteProfile() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("update-user")
                .name("Original Name")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        AthleteProfile saved = repository.save(profile);
        UUID profileId = saved.getId();
        Instant originalCreatedAt = saved.getCreatedAt();

        // When — simulate service setting updatedAt on update
        saved.setName("Updated Name");
        saved.setAge(31);
        saved.setWeight(72.0);
        saved.setUpdatedAt(NOW.plusSeconds(60));

        AthleteProfile updated = repository.save(saved);

        // Then
        assertThat(updated.getId()).isEqualTo(profileId);
        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getAge()).isEqualTo(31);
        assertThat(updated.getWeight()).isEqualTo(72.0);
        assertThat(updated.getCreatedAt()).isEqualTo(originalCreatedAt);
        assertThat(updated.getUpdatedAt()).isAfter(originalCreatedAt);
    }

    @Test
    void shouldDeleteAthleteProfile() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("delete-user")
                .name("To Be Deleted")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        AthleteProfile saved = repository.save(profile);
        String userId = saved.getUserId();

        // When
        repository.delete(saved);

        // Then
        Optional<AthleteProfile> found = repository.findByUserId(userId);
        assertThat(found).isEmpty();
    }

    @Test
    void shouldReturnTrueIfUserExists() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("test-runner")
                .name("Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        repository.save(profile);

        // When
        boolean existsTestRunnerUser = repository.existsByUserId("test-runner");

        // Then
        assertTrue(existsTestRunnerUser);
    }

    @Test
    void shouldReturnFalseIfUserDoesNotExist() {
        // Given
        AthleteProfile profile = AthleteProfile.builder()
                .userId("test-runner")
                .name("Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();

        repository.save(profile);

        // When
        boolean existsTestRunnerUser = repository.existsByUserId("not-existing-user");

        // Then
        assertFalse(existsTestRunnerUser);
    }

}