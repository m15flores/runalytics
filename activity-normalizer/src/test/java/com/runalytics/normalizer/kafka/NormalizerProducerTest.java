package com.runalytics.normalizer.kafka;

import com.runalytics.normalizer.dto.ActivityNormalizedDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NormalizerProducerTest {

    @Mock
    private KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @InjectMocks
    private NormalizerProducer producer;

    @Test
    void shouldPublishActivityNormalizedDto() {
        // Given
        ActivityNormalizedDto dto = new ActivityNormalizedDto(
                UUID.randomUUID(),
                "user-12345",
                "Garmin-Fenix-7-Pro",
                Instant.now(),
                2780,
                new BigDecimal("10042.50"),
                List.of(),
                Instant.now()
        );

        // Mock devuelve CompletableFuture vacío (no falla)
        when(kafkaTemplate.send(anyString(), anyString(), any(ActivityNormalizedDto.class)))
                .thenReturn(new CompletableFuture<>());

        ReflectionTestUtils.setField(producer, "normalizedTopic", "activities.normalized");

        // When
        producer.publish(dto);

        // Then
        verify(kafkaTemplate, times(1)).send(
                eq("activities.normalized"),
                eq("user-12345"),
                eq(dto)
        );
    }
}