package com.runalitycs.report_generator.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalitycs.report_generator.dto.AthleteProfileDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.mapper.AthleteProfileMapperImpl;
import com.runalitycs.report_generator.service.AthleteProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AthleteProfileController.class)
@Import(AthleteProfileMapperImpl.class)
public class AthleteProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AthleteProfileService athleteProfileService;

    @Test
    void shouldCreateProfile() throws Exception {
        // Given
        AthleteProfileDto requestDto = new AthleteProfileDto(
                null,
                "test-user",
                "Test Runner",
                30,
                70.0,
                190,
                null,
                null,
                null
        );

        AthleteProfile savedProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();

        when(athleteProfileService.createProfile(any(AthleteProfile.class)))
                .thenReturn(savedProfile);

        // When & Then
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value("test-user"))
                .andExpect(jsonPath("$.name").value("Test Runner"))
                .andExpect(jsonPath("$.age").value(30))
                .andExpect(jsonPath("$.weight").value(70.0))
                .andExpect(jsonPath("$.maxHeartRate").value(190));
    }

    @Test
    void shouldReturn409WhenProfileAlreadyExists() throws Exception {
        // Given
        AthleteProfileDto requestDto = new AthleteProfileDto(
                null, "existing-user", "Test Runner", 30, 70.0, 190, null, null, null
        );

        when(athleteProfileService.createProfile(any(AthleteProfile.class)))
                .thenThrow(new IllegalArgumentException("Profile already exists for userId: existing-user"));

        // When & Then
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn400WhenInvalidData() throws Exception {
        // Given
        AthleteProfileDto invalidDto = new AthleteProfileDto(
                null,
                "", // Empty userId (invalid)
                "", // Empty name (invalid)
                5,  // Age too low (invalid)
                -10.0, // Negative weight (invalid)
                50, // Heart rate too low (invalid)
                null,
                null,
                null
        );

        // When & Then
        mockMvc.perform(post("/api/profiles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetProfileByUserId() throws Exception {
        // Given
        AthleteProfile existingProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("mario-runner")
                .name("Mario")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .build();

        when(athleteProfileService.getProfileByUserId("mario-runner"))
                .thenReturn(existingProfile);

        // When & Then
        mockMvc.perform(get("/api/profiles/user/mario-runner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("mario-runner"))
                .andExpect(jsonPath("$.name").value("Mario"))
                .andExpect(jsonPath("$.age").value(30));
    }

    @Test
    void shouldReturn404WhenNotFound() throws Exception {
        // Given
        when(athleteProfileService.getProfileByUserId("non-existent"))
                .thenThrow(new IllegalArgumentException("Profile not found for userId: non-existent"));

        // When & Then
        mockMvc.perform(get("/api/profiles/user/non-existent"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldUpdateProfile() throws Exception {
        // Given
        AthleteProfileDto updateDto = new AthleteProfileDto(
                null,
                "test-user",
                "Updated Name",
                31,
                72.0,
                185,
                null,
                null,
                null
        );

        AthleteProfile updatedProfile = AthleteProfile.builder()
                .id(UUID.randomUUID())
                .userId("test-user")
                .name("Updated Name")
                .age(31)
                .weight(72.0)
                .maxHeartRate(185)
                .build();

        when(athleteProfileService.updateProfile(any(String.class), any(AthleteProfile.class)))
                .thenReturn(updatedProfile);

        // When & Then
        mockMvc.perform(put("/api/profiles/user/test-user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.age").value(31))
                .andExpect(jsonPath("$.weight").value(72.0));
    }

    @Test
    void shouldDeleteProfile() throws Exception {
        // Given
        doNothing().when(athleteProfileService).deleteProfile("test-user");

        // When & Then
        mockMvc.perform(delete("/api/profiles/user/test-user"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldGetAllProfiles() throws Exception {
        // Given
        List<AthleteProfile> profiles = List.of(
                AthleteProfile.builder()
                        .id(UUID.randomUUID())
                        .userId("user-1")
                        .name("User One")
                        .build(),
                AthleteProfile.builder()
                        .id(UUID.randomUUID())
                        .userId("user-2")
                        .name("User Two")
                        .build()
        );

        when(athleteProfileService.getAllProfiles()).thenReturn(profiles);

        // When & Then
        mockMvc.perform(get("/api/profiles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value("user-1"))
                .andExpect(jsonPath("$[1].userId").value("user-2"));
    }
}
