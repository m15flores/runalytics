package com.runalytics.activity.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ActivityDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldCreateValidActivityDto() {
        // Given
        String userId = "user-12345";
        String device = "Garmin-Fenix-7-Pro";
        Instant timestamp = Instant.parse("2025-01-01T10:30:00Z");
        String source = "garmin-mock";
        Map<String, Object> raw = Map.of(
                "distance_m", 10042,
                "duration_s", 2780,
                "pace_samples", new int[]{300, 298, 305},
                "hr_samples", new int[]{145, 148, 150}
        );

        // When
        ActivityDto dto = new ActivityDto(userId, device, timestamp, source, raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "DTO should be valid");
        assertEquals(userId, dto.userId());
        assertEquals(device, dto.device());
        assertEquals(timestamp, dto.timestamp());
        assertEquals(source, dto.source());
        assertEquals(raw, dto.raw());
    }

    @Test
    void shouldFailValidationWhenUserIdIsNull() {
        // Given
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto(null, "device", timestamp, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("userId", violation.getPropertyPath().toString());
        assertEquals("userId cannot be blank", violation.getMessage());
    }

    @Test
    void shouldFailValidationWhenUserIdIsBlank() {
        // Given
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto("", "device", timestamp, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("userId", violation.getPropertyPath().toString());
        assertEquals("userId cannot be blank", violation.getMessage());
    }

    @Test
    void shouldFailValidationWhenUserIdIsWhitespace() {
        // Given
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto("   ", "device", timestamp, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("userId", violation.getPropertyPath().toString());
        assertEquals("userId cannot be blank", violation.getMessage());
    }

    @Test
    void shouldFailValidationWhenTimestampIsNull() {
        // Given
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto("userId", "device", null, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("timestamp", violation.getPropertyPath().toString());
        assertEquals("timestamp cannot be null", violation.getMessage());
    }

    @Test
    void shouldFailValidationWhenRawIsNull() {
        // Given
        Instant timestamp = Instant.now();

        // When
        ActivityDto dto = new ActivityDto("userId", "device", timestamp, "source", null);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("raw", violation.getPropertyPath().toString());
        assertEquals("raw data cannot be empty", violation.getMessage());
    }

    @Test
    void shouldFailValidationWhenRawIsEmpty() {
        // Given
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of();

        // When
        ActivityDto dto = new ActivityDto("userId", "device", timestamp, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertFalse(violations.isEmpty(), "Should have validation errors");
        assertEquals(1, violations.size());

        ConstraintViolation<ActivityDto> violation = violations.iterator().next();
        assertEquals("raw", violation.getPropertyPath().toString());
        assertEquals("raw data cannot be empty", violation.getMessage());
    }

    @Test
    void shouldAllowNullDevice() {
        // Given
        String userId = "user-12345";
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto(userId, null, timestamp, "source", raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Device can be null");
        assertNull(dto.device());
    }

    @Test
    void shouldAllowNullSource() {
        // Given
        String userId = "user-12345";
        Instant timestamp = Instant.now();
        Map<String, Object> raw = Map.of("test", "data");

        // When
        ActivityDto dto = new ActivityDto(userId, "device", timestamp, null, raw);
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertTrue(violations.isEmpty(), "Source can be null");
        assertNull(dto.source());
    }

    @Test
    void shouldFailValidationWithMultipleErrors() {
        // Given / When
        ActivityDto dto = new ActivityDto(null, "device", null, "source", Map.of());
        Set<ConstraintViolation<ActivityDto>> violations = validator.validate(dto);

        // Then
        assertEquals(3, violations.size(), "Should have 3 validation errors (userId, timestamp, raw)");
    }
}