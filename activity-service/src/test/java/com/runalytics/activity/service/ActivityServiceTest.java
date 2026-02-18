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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ActivityServiceTest {

    // Crear mock
    @Mock
    private ActivityProducer activityProducer;

    // Inyectar en servicio
    @InjectMocks
    private ActivityService activityService;

    @Test
    void shouldIngestActivitySuccessfully() {
        //Given
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

        //When
        activityService.ingestActivity(dto);

        //Then
        verify(activityProducer, times(1)).publishActivity(any(ActivityDto.class));
    }

    @Test
    void shouldThrowExceptionWhenDtoIsNull() {
        //Given
        ActivityDto dto = null;

        //When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> activityService.ingestActivity(dto)
        );

        //Then
        assertEquals("ActivityDto cannot be null", exception.getMessage());
        verify(activityProducer, never()).publishActivity(any(ActivityDto.class));
    }

    @Test
    void shouldReturnUserIdAfterIngestion() {
        //Given
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

        //When
        String result = activityService.ingestActivity(dto);

        //Then
        assertEquals("user-12345", result);
    }

    @Test
    void shouldPublishToCorrectKafkaTopic() {
        //Given
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

        //When
        activityService.ingestActivity(dto);

        //Then
        //Capturamos el argumento pasado al producer
        ArgumentCaptor<ActivityDto> captor = ArgumentCaptor.forClass(ActivityDto.class);
        verify(activityProducer).publishActivity(captor.capture());

        // Verificamos que el DTO capturado es el correcto
        ActivityDto capturedDto = captor.getValue();
        assertEquals("user-12345", capturedDto.userId());
        assertEquals("Garmin-Fenix-7-Pro", capturedDto.device());
        assertEquals("garmin-mock", capturedDto.source());
    }
}
