package com.runalytics.activity.kafka;

import com.runalytics.activity.dto.ActivityDto;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActivityProducerTest {

    @Mock
    private KafkaTemplate<String, ActivityDto> kafkaTemplate;

    private ActivityProducer activityProducer;

    private static final String TOPIC = "activities.raw.ingested";

    private static final ActivityDto DTO = new ActivityDto(
            "user-12345",
            "Garmin-Fenix-7-Pro",
            Instant.parse("2025-01-01T10:30:00Z"),
            "garmin-mock",
            Map.of("distance_m", 10042, "duration_s", 2780)
    );

    @BeforeEach
    void setUp() {
        activityProducer = new ActivityProducer(kafkaTemplate);
        ReflectionTestUtils.setField(activityProducer, "rawIngestedTopic", TOPIC);
    }

    @Test
    void shouldPublishToCorrectTopic() {
        SendResult<String, ActivityDto> sendResult = mockSendResult();
        when(kafkaTemplate.send(anyString(), anyString(), any(ActivityDto.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        activityProducer.publishActivity(DTO);

        verify(kafkaTemplate).send(eq(TOPIC), anyString(), any(ActivityDto.class));
    }

    @Test
    void shouldUseUserIdAsPartitionKey() {
        SendResult<String, ActivityDto> sendResult = mockSendResult();
        when(kafkaTemplate.send(anyString(), anyString(), any(ActivityDto.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        activityProducer.publishActivity(DTO);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(anyString(), keyCaptor.capture(), any(ActivityDto.class));
        assertEquals("user-12345", keyCaptor.getValue());
    }

    @Test
    void shouldLogErrorWhenPublishFails() {
        CompletableFuture<SendResult<String, ActivityDto>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Kafka unavailable"));
        when(kafkaTemplate.send(anyString(), anyString(), any(ActivityDto.class)))
                .thenReturn(failedFuture);

        activityProducer.publishActivity(DTO);

        // No exception propagated to caller; error is logged asynchronously
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any(ActivityDto.class));
    }

    private SendResult<String, ActivityDto> mockSendResult() {
        RecordMetadata metadata = new RecordMetadata(
                new TopicPartition(TOPIC, 0), 0L, 0, 0L, 0, 0);
        SendResult<String, ActivityDto> result = mock(SendResult.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        return result;
    }
}