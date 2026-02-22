package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.repository.AthleteProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AthleteProfileServiceTest {

    @Mock
    private AthleteProfileRepository repository;

    @Mock
    private Clock clock;

    @InjectMocks
    private AthleteProfileService service;

    private static final Instant FIXED_NOW = Instant.parse("2024-12-10T12:00:00Z");

    private AthleteProfile testProfile;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_NOW);
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));

        testProfile = AthleteProfile.builder()
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();
    }

    @Test
    void shouldCreateAthleteProfile() {
        // Given
        AthleteProfile savedProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();

        when(repository.existsByUserId("test-user")).thenReturn(false);
        when(repository.save(any(AthleteProfile.class))).thenReturn(savedProfile);

        // When
        AthleteProfile created = service.createProfile(testProfile);

        // Then
        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo("test-user");
        assertThat(created.getName()).isEqualTo("Test Runner");

        verify(repository).existsByUserId("test-user");
        verify(repository).save(testProfile);
    }

    @Test
    void shouldThrowExceptionWhenProfileAlreadyExists() {
        // Given
        when(repository.existsByUserId("test-user")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> service.createProfile(testProfile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile already exists");

        verify(repository).existsByUserId("test-user");
        verify(repository, never()).save(any());
    }

    @Test
    void shouldGetProfileByUserId() {
        // Given
        AthleteProfile existingProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Test Runner")
                .build();

        when(repository.findByUserId("test-user"))
                .thenReturn(Optional.of(existingProfile));

        // When
        AthleteProfile found = service.getProfileByUserId("test-user");

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getUserId()).isEqualTo("test-user");
        assertThat(found.getName()).isEqualTo("Test Runner");

        verify(repository).findByUserId("test-user");
    }

    @Test
    void shouldThrowExceptionWhenProfileNotFound() {
        // Given
        when(repository.findByUserId("non-existent"))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> service.getProfileByUserId("non-existent"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Profile not found");

        verify(repository).findByUserId("non-existent");
    }

    @Test
    void shouldUpdateProfile() {
        // Given
        AthleteProfile existingProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Old Name")
                .age(30)
                .build();

        AthleteProfile updatedProfile = AthleteProfile.builder()
                .userId("test-user")
                .name("New Name")
                .age(31)
                .weight(75.0)
                .build();

        AthleteProfile savedProfile = AthleteProfile.builder()
                .id(existingProfile.getId())
                .userId("test-user")
                .name("New Name")
                .age(31)
                .weight(75.0)
                .build();

        when(repository.findByUserId("test-user"))
                .thenReturn(Optional.of(existingProfile));
        when(repository.save(any(AthleteProfile.class)))
                .thenReturn(savedProfile);

        // When
        AthleteProfile updated = service.updateProfile("test-user", updatedProfile);

        // Then
        assertThat(updated.getName()).isEqualTo("New Name");
        assertThat(updated.getAge()).isEqualTo(31);
        assertThat(updated.getWeight()).isEqualTo(75.0);

        verify(repository).findByUserId("test-user");
        verify(repository).save(any(AthleteProfile.class));
    }

    @Test
    void shouldDeleteProfile() {
        // Given
        AthleteProfile existingProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Test Runner")
                .build();

        when(repository.findByUserId("test-user"))
                .thenReturn(Optional.of(existingProfile));
        doNothing().when(repository).delete(existingProfile);

        // When
        service.deleteProfile("test-user");

        // Then
        verify(repository).findByUserId("test-user");
        verify(repository).delete(existingProfile);
    }

    @Test
    void shouldCheckIfProfileExists() {
        // Given
        when(repository.existsByUserId("existing-user")).thenReturn(true);
        when(repository.existsByUserId("non-existent")).thenReturn(false);

        // When & Then
        assertThat(service.profileExists("existing-user")).isTrue();
        assertThat(service.profileExists("non-existent")).isFalse();

        verify(repository).existsByUserId("existing-user");
        verify(repository).existsByUserId("non-existent");
    }
}