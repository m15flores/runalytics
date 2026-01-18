package com.runalitycs.report_generator.controller;

import com.runalitycs.report_generator.dto.AthleteProfileDto;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.mapper.AthleteProfileMapper;
import com.runalitycs.report_generator.service.AthleteProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/profiles")
@RequiredArgsConstructor
@Slf4j
public class AthleteProfileController {

    private final AthleteProfileService athleteProfileService;
    private final AthleteProfileMapper mapper;

    @PostMapping
    public ResponseEntity<AthleteProfileDto> createProfile(@Valid @RequestBody AthleteProfileDto dto) {
        log.info("POST /api/profiles - Creating profile for userId: {}", dto.userId());

        AthleteProfile entity = mapper.toEntity(dto);
        AthleteProfile created = athleteProfileService.createProfile(entity);
        AthleteProfileDto responseDto = mapper.toDto(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    @GetMapping("user/{userId}")
    public ResponseEntity<AthleteProfileDto> getProfileByUserId(@PathVariable String userId) {
        log.info("GET /api/profiles/user/{} - Fetching profile", userId);

        AthleteProfile profile = athleteProfileService.getProfileByUserId(userId);
        AthleteProfileDto dto = mapper.toDto(profile);

        return ResponseEntity.ok(dto);
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<AthleteProfileDto> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody AthleteProfileDto dto) {

        log.info("PUT /api/v1/profiles/user/{} - Updating profile", userId);

        AthleteProfile entity = mapper.toEntity(dto);
        AthleteProfile updated = athleteProfileService.updateProfile(userId, entity);
        AthleteProfileDto responseDto = mapper.toDto(updated);

        return ResponseEntity.ok(responseDto);
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<Void> deleteProfile(@PathVariable String userId) {
        log.info("DELETE /api/v1/profiles/user/{} - Deleting profile", userId);

        athleteProfileService.deleteProfile(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AthleteProfileDto>> getAllProfiles() {
        log.info("GET /api/v1/profiles - Fetching all profiles");

        List<AthleteProfile> profiles = athleteProfileService.getAllProfiles();
        List<AthleteProfileDto> dtos = profiles.stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity.ok(dtos);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex) {

        log.error("Validation error: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                "NOT_FOUND",
                ex.getMessage(),
                System.currentTimeMillis()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // Inner record for error responses
    private record ErrorResponse(String code, String message, long timestamp) {}
}
