package com.runalitycs.report_generator.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class AthleteProfileDtoTest {

    private Validator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules(); // For Java 8 time support
    }

    @Test
    void shouldCreateValidDto() {
        // Given & When
        AthleteProfileDto dto = new AthleteProfileDto(
                UUID.randomUUID(),
                "test-user",
                "Test Runner",
                30,
                70.0,
                190,
                null,
                Instant.now(),
                Instant.now()
        );

        // Then
        assertNotNull(dto);
        assertEquals("test-user", dto.userId());
        assertEquals("Test Runner", dto.name());
        assertEquals(30, dto.age());
        assertEquals(70.0, dto.weight());
        assertEquals(190, dto.maxHeartRate());
    }

    @Test
    void shouldCreateDtoWithMinimalFields() {
        // Given & When - Only required fields
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "minimal-user",
                "Minimal Profile",
                null,
                null,
                null,
                null,
                null,
                null
        );

        // Then
        assertNull(dto.id());
        assertEquals("minimal-user", dto.userId());
        assertEquals("Minimal Profile", dto.name());
        assertNull(dto.age());
        assertNull(dto.weight());
        assertNull(dto.maxHeartRate());
    }

    @Test
    void shouldFailValidationWhenUserIdIsBlank() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "", // Blank userId
                "Test Runner",
                30,
                70.0,
                190,
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("userId") &&
                        v.getMessage().contains("userId is required")
        );
    }

    @Test
    void shouldFailValidationWhenNameIsBlank() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "", // Blank name
                30,
                70.0,
                190,
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("name") &&
                        v.getMessage().contains("name is required")
        );
    }

    @Test
    void shouldFailValidationWhenAgeTooLow() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                5, // Age too low
                70.0,
                190,
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("age") &&
                        v.getMessage().contains("age must be at least 10")
        );
    }

    @Test
    void shouldFailValidationWhenAgeTooHigh() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                150, // Age too high
                70.0,
                190,
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("age") &&
                        v.getMessage().contains("age must be at most 120")
        );
    }

    @Test
    void shouldFailValidationWhenWeightIsNegative() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                30,
                -10.0, // Negative weight
                190,
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("weight") &&
                        v.getMessage().contains("weight must be positive")
        );
    }

    @Test
    void shouldFailValidationWhenMaxHeartRateTooLow() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                30,
                70.0,
                50, // Heart rate too low
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("maxHeartRate") &&
                        v.getMessage().contains("maxHeartRate must be at least 100")
        );
    }

    @Test
    void shouldFailValidationWhenMaxHeartRateTooHigh() {
        // Given
        AthleteProfileDto dto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                30,
                70.0,
                300, // Heart rate too high
                null,
                null,
                null
        );

        // When
        Set<ConstraintViolation<AthleteProfileDto>> violations = validator.validate(dto);

        // Then
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v ->
                v.getPropertyPath().toString().equals("maxHeartRate") &&
                        v.getMessage().contains("maxHeartRate must be at most 250")
        );
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        // Given
        UUID id = UUID.randomUUID();
        Instant now = Instant.parse("2024-12-08T10:00:00Z");

        AthleteProfileDto dto = new AthleteProfileDto(
                id,
                "test-user",
                "Test Runner",
                30,
                70.0,
                190,
                null,
                now,
                now
        );

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then
        assertThat(json).contains("\"userId\":\"test-user\"");
        assertThat(json).contains("\"name\":\"Test Runner\"");
        assertThat(json).contains("\"age\":30");
        assertThat(json).contains("\"weight\":70.0");
        assertThat(json).contains("\"maxHeartRate\":190");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        // Given
        String json = """
                {
                    "userId": "test-user",
                    "name": "Test Runner",
                    "age": 30,
                    "weight": 70.0,
                    "maxHeartRate": 190
                }
                """;

        // When
        AthleteProfileDto dto = objectMapper.readValue(json, AthleteProfileDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("test-user", dto.userId());
        assertEquals("Test Runner", dto.name());
        assertEquals(30, dto.age());
        assertEquals(70.0, dto.weight());
        assertEquals(190, dto.maxHeartRate());
    }

    @Test
    void shouldHandleNullOptionalFieldsInJson() throws Exception {
        // Given
        String json = """
                {
                    "userId": "minimal-user",
                    "name": "Minimal Profile"
                }
                """;

        // When
        AthleteProfileDto dto = objectMapper.readValue(json, AthleteProfileDto.class);

        // Then
        assertNotNull(dto);
        assertEquals("minimal-user", dto.userId());
        assertEquals("Minimal Profile", dto.name());
        assertNull(dto.age());
        assertNull(dto.weight());
        assertNull(dto.maxHeartRate());
    }
}