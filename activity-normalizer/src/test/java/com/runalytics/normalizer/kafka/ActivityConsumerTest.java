package com.runalytics.normalizer.kafka;

import com.runalytics.normalizer.dto.ActivityNormalizedDto;
import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.runalytics.normalizer.service.ActivityNormalizerService;
import com.runalytics.normalizer.service.FitParserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityConsumerTest {

    @Spy
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private ActivityNormalizerService normalizerService;

    @Mock
    private NormalizerProducer normalizerProducer;

    @Mock
    private FitParserService fitParserService;

    @InjectMocks
    private ActivityConsumer consumer;

    @Test
    void shouldConsumeAndProcessActivityMessage() {
        // Given
        String message = """
            {
                "userId": "user-12345",
                "device": "Garmin-Fenix-7-Pro",
                "timestamp": "2025-01-01T10:30:00Z",
                "source": "garmin-mock",
                "raw": {
                    "duration_s": 2780,
                    "distance_m": 10042
                }
            }
            """;

        ParsedFitData parsedData = new ParsedFitData(
                Instant.parse("2025-01-01T10:30:00Z"),
                2780,
                new BigDecimal("10042"),
                List.of(
                        new ActivitySample(
                                Instant.parse("2025-01-01T10:30:05Z"),
                                40.416775, -3.703790, 145, 300, 650.5, 85
                        )
                )
        );

        ActivityNormalizedDto normalizedDto = new ActivityNormalizedDto(
                UUID.randomUUID(),
                "user-12345",
                "Garmin-Fenix-7-Pro",
                Instant.parse("2025-01-01T10:30:00Z"),
                2780,
                new BigDecimal("10042"),
                parsedData.samples(),
                Instant.now()
        );

        when(normalizerService.normalize(eq("user-12345"), eq("Garmin-Fenix-7-Pro"), any(ParsedFitData.class)))
                .thenReturn(normalizedDto);

        // When
        consumer.consume(message);

        // Then
        verify(normalizerService, times(1)).normalize(
                eq("user-12345"),
                eq("Garmin-Fenix-7-Pro"),
                any(ParsedFitData.class)
        );
        verify(normalizerProducer, times(1)).publish(normalizedDto);
    }

    @Test
    void shouldHandleInvalidJsonGracefully() {
        // Given
        String invalidJson = "{ invalid json }";

        // When
        consumer.consume(invalidJson);

        // Then
        verify(normalizerService, never()).normalize(any(), any(), any());
        verify(normalizerProducer, never()).publish(any());
    }

    @Test
    void shouldUseFitParserServiceWhenFitBase64Present() throws Exception {
        byte[] fakeFitBytes = "fakefitbytes".getBytes();
        String base64 = Base64.getEncoder().encodeToString(fakeFitBytes);

        String message = """
            {
                "userId": "mario-001",
                "device": "Garmin Fenix",
                "timestamp": "2026-02-24T07:30:00Z",
                "source": "garmin",
                "raw": {
                    "fitBase64": "%s"
                }
            }
            """.formatted(base64);

        ParsedFitData parsedData = new ParsedFitData(
                Instant.parse("2026-02-24T07:30:00Z"),
                3600,
                new BigDecimal("11000"),
                List.of(new ActivitySample(Instant.parse("2026-02-24T07:30:05Z"), null, null, 145, null, null, null))
        );
        ActivityNormalizedDto normalizedDto = new ActivityNormalizedDto(
                UUID.randomUUID(), "mario-001", "Garmin Fenix",
                Instant.parse("2026-02-24T07:30:00Z"), 3600, new BigDecimal("11000"),
                parsedData.samples(), Instant.now()
        );

        when(fitParserService.parse(any(InputStream.class))).thenReturn(parsedData);
        when(normalizerService.normalize(eq("mario-001"), eq("Garmin Fenix"), eq(parsedData)))
                .thenReturn(normalizedDto);

        consumer.consume(message);

        verify(fitParserService, times(1)).parse(any(InputStream.class));
        verify(normalizerService, times(1)).normalize("mario-001", "Garmin Fenix", parsedData);
        verify(normalizerProducer, times(1)).publish(normalizedDto);
    }

    @Test
    void shouldHandleNormalizationErrorGracefully() {
        // Given
        String message = """
            {
                "userId": "user-12345",
                "device": "Garmin-Fenix-7-Pro",
                "timestamp": "2025-01-01T10:30:00Z",
                "source": "garmin-mock",
                "raw": {
                    "duration_s": 2780,
                    "distance_m": 10042
                }
            }
            """;

        when(normalizerService.normalize(any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // When
        consumer.consume(message);

        // Then
        verify(normalizerService, times(1)).normalize(any(), any(), any());
        verify(normalizerProducer, never()).publish(any());
    }
}