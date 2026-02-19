package com.runalytics.normalizer.service;

import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FitParserServiceTest {

    private FitParserService fitParserService;

    @BeforeEach
    void setUp() {
        fitParserService = new FitParserService();
    }

    @Test
    void shouldParseFitFile() throws IOException {
        // Given
        InputStream fitFile = new ClassPathResource("fit-files/20994667289_ACTIVITY.fit")
                .getInputStream();
        BigDecimal zeroBigDecimal = new BigDecimal(0);
        // When
        ParsedFitData result = fitParserService.parse(fitFile);

        // Then
        assertNotNull(result);
        assertNotNull(result.startedAt());
        assertTrue(result.durationSeconds() > 0);
        assertTrue(result.distanceMeters().compareTo(BigDecimal.ZERO) > 0);
        assertFalse(result.samples().isEmpty());
    }

    @Test
    void shouldExtractSamplesWithTimestamps() throws IOException {
        // Given
        InputStream fitFile = new ClassPathResource("fit-files/20994667289_ACTIVITY.fit")
                .getInputStream();

        // When
        ParsedFitData result = fitParserService.parse(fitFile);
        List<ActivitySample> samples = result.samples();

        // Then
        assertFalse(samples.isEmpty());

        ActivitySample firstSample = samples.get(0);
        assertNotNull(firstSample.timestamp());

        // Verificar que al menos algunos samples tienen datos
        boolean hasHeartRate = samples.stream()
                .anyMatch(s -> s.heartRate() != null && s.heartRate() > 0);
        boolean hasPosition = samples.stream()
                .anyMatch(s -> s.latitude() != null && s.longitude() != null);

        assertTrue(hasHeartRate || hasPosition,
                "Should have at least heart rate or position data");
    }

    @Test
    void shouldHandleEmptyOrInvalidFile() {
        // Given
        InputStream emptyStream = InputStream.nullInputStream();

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> fitParserService.parse(emptyStream));
    }

    @Test
    void shouldExtractMetadata() throws IOException {
        // Given
        InputStream fitFile = new ClassPathResource("fit-files/20994667289_ACTIVITY.fit")
                .getInputStream();

        // When
        ParsedFitData result = fitParserService.parse(fitFile);

        // Then
        assertNotNull(result.startedAt());
        assertTrue(result.durationSeconds() > 0, "Duration should be positive");
        assertTrue(result.distanceMeters().doubleValue() > 0, "Distance should be positive");
    }
}