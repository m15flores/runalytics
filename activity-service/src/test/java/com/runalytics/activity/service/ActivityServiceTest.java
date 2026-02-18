package com.runalytics.activity.service;

import com.runalytics.activity.dto.ActivityDto;
import com.runalytics.activity.kafka.ActivityProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

    @Mock
    private ActivityProducer activityProducer;

    @InjectMocks
    private ActivityService activityService;

    @Test
    void shouldIngestActivitySuccessfully() {
        ActivityDto dto = new ActivityDto(
                "user-12345",
                "Garmin-Fenix-7-Pro",
                Instant.parse("2025-01-01T10:30:00Z"),
                "garmin-mock",
                Map.of(
                        "distance_m", 10042,
                        "duration_s", 2780
                )
        );

        activityService.ingestActivity(dto);

        verify(activityProducer, times(1)).publishActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturnUserIdAfterIngestion() {
        ActivityDto dto = new ActivityDto(
                "user-12345",
                "Garmin-Fenix-7-Pro",
                Instant.parse("2025-01-01T10:30:00Z"),
                "garmin-mock",
                Map.of(
                        "distance_m", 10042,
                        "duration_s", 2780
                )
        );

        String result = activityService.ingestActivity(dto);

        assertEquals("user-12345", result);
    }

    @Test
    void shouldPassCorrectDtoToProducer() {
        ActivityDto dto = new ActivityDto(
                "user-12345",
                "Garmin-Fenix-7-Pro",
                Instant.parse("2025-01-01T10:30:00Z"),
                "garmin-mock",
                Map.of(
                        "distance_m", 10042,
                        "duration_s", 2780
                )
        );

        activityService.ingestActivity(dto);

        ArgumentCaptor<ActivityDto> captor = ArgumentCaptor.forClass(ActivityDto.class);
        verify(activityProducer).publishActivity(captor.capture());

        ActivityDto capturedDto = captor.getValue();
        assertEquals("user-12345", capturedDto.userId());
        assertEquals("Garmin-Fenix-7-Pro", capturedDto.device());
        assertEquals("garmin-mock", capturedDto.source());
    }
}