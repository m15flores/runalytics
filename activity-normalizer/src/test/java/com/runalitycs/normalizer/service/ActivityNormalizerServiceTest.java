package com.runalitycs.normalizer.service;

import com.runalitycs.normalizer.dto.ActivityNormalizedDto;
import com.runalitycs.normalizer.dto.ActivitySample;
import com.runalitycs.normalizer.dto.ParsedFitData;
import com.runalitycs.normalizer.entity.Activity;
import com.runalitycs.normalizer.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityNormalizerServiceTest {

    @Mock
    private FitParserService fitParserService;

    @Mock
    private ActivityRepository activityRepository;

    @InjectMocks
    private ActivityNormalizerService normalizerService;

    @Test
    void shouldNormalizeActivityFromParsedFitData() {
        // Given
        String userId = "user-12345";
        String device = "Garmin-Fenix-7-Pro";

        ParsedFitData parsedData = new ParsedFitData(
                Instant.parse("2025-01-01T10:30:00Z"),
                2780,
                new BigDecimal("10042.50"),
                List.of(
                        new ActivitySample(
                                Instant.parse("2025-01-01T10:30:05Z"),
                                40.416775, -3.703790, 145, 300, 650.5, 85
                        )
                )
        );

        Activity savedActivity = new Activity();
        savedActivity.setId(UUID.randomUUID());
        savedActivity.setUserId(userId);
        savedActivity.setDevice(device);
        savedActivity.setStartedAt(parsedData.startedAt());
        savedActivity.setDurationSeconds(parsedData.durationSeconds());
        savedActivity.setDistanceMeters(parsedData.distanceMeters());
        savedActivity.setSamples(parsedData.samples());

        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // When
        ActivityNormalizedDto result = normalizerService.normalize(
                userId, device, parsedData
        );

        // Then
        assertNotNull(result);
        assertEquals(savedActivity.getId(), result.activityId());
        assertEquals(userId, result.userId());
        assertEquals(device, result.device());
        assertEquals(parsedData.startedAt(), result.startedAt());
        assertEquals(1, result.samples().size());
        assertNotNull(result.normalizedAt());

        // Verificar que se guardó en DB
        verify(activityRepository, times(1)).save(any(Activity.class));
    }

    @Test
    void shouldSetCreatedAtAndUpdatedAtOnSave() {
        // Given
        String userId = "user-12345";
        ParsedFitData parsedData = new ParsedFitData(
                Instant.now(),
                1000,
                new BigDecimal("5000"),
                List.of()
        );

        Activity savedActivity = new Activity();
        savedActivity.setId(UUID.randomUUID());
        savedActivity.setUserId(userId);
        savedActivity.setStartedAt(parsedData.startedAt());

        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // When
        normalizerService.normalize(userId, null, parsedData);

        // Then
        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(activityCaptor.capture());

        Activity capturedActivity = activityCaptor.getValue();
        assertEquals(userId, capturedActivity.getUserId());
        assertEquals(parsedData.startedAt(), capturedActivity.getStartedAt());
        assertEquals(parsedData.durationSeconds(), capturedActivity.getDurationSeconds());
    }

    @Test
    void shouldHandleNullDevice() {
        // Given
        String userId = "user-12345";
        ParsedFitData parsedData = new ParsedFitData(
                Instant.now(),
                1000,
                new BigDecimal("5000"),
                List.of()
        );

        Activity savedActivity = new Activity();
        savedActivity.setId(UUID.randomUUID());
        savedActivity.setUserId(userId);
        savedActivity.setStartedAt(parsedData.startedAt());

        when(activityRepository.save(any(Activity.class))).thenReturn(savedActivity);

        // When
        ActivityNormalizedDto result = normalizerService.normalize(userId, null, parsedData);

        // Then
        assertNotNull(result);
        assertNull(result.device());
    }

    @Test
    void shouldThrowExceptionWhenUserIdIsNull() {
        // Given
        ParsedFitData parsedData = new ParsedFitData(
                Instant.now(),
                1000,
                new BigDecimal("5000"),
                List.of()
        );

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> normalizerService.normalize(null, "device", parsedData));

        verify(activityRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenParsedDataIsNull() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> normalizerService.normalize("user-12345", "device", null));

        verify(activityRepository, never()).save(any());
    }
}