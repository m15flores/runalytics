package com.runalitycs.report_generator.producer;

import com.runalitycs.report_generator.dto.ReportGeneratedEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReportGeneratedProducerTest {

    @Mock
    private KafkaTemplate<String, ReportGeneratedEventDto> kafkaTemplate;

    @InjectMocks
    private ReportGeneratedProducer producer;

    private ReportGeneratedEventDto event;

    @BeforeEach
    void setUp() {
        // Set the topic value using reflection
        ReflectionTestUtils.setField(producer, "topic", "reports.generated");

        // KafkaTemplate.send() returns a CompletableFuture; without a stub it returns null,
        // which causes a NullPointerException when the producer chains .whenComplete() on it.
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(CompletableFuture.completedFuture(new SendResult<>(null, null)));

        event = ReportGeneratedEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{\"totalKm\": 52.0}")
                .generatedAt(Instant.now())
                .triggerActivityId(UUID.randomUUID())
                .athleteName("Test Runner")
                .currentGoal("Marathon sub-3:30")
                .build();
    }

    @Test
    void shouldPublishReportGeneratedEvent() {
        // When
        producer.publish(event);

        // Then
        verify(kafkaTemplate).send(eq("reports.generated"), eq("test-user"), eq(event));
    }

    @Test
    void shouldUseUserIdAsKey() {
        // Given
        String userId = "specific-user-123";
        ReportGeneratedEventDto eventWithSpecificUser = ReportGeneratedEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId(userId)
                .weekNumber(50)
                .year(2024)
                .summaryJson("{}")
                .generatedAt(Instant.now())
                .triggerActivityId(UUID.randomUUID())
                .athleteName("Test Runner")
                .currentGoal("Sub-3h marathon")
                .build();

        // When
        producer.publish(eventWithSpecificUser);

        // Then - Should use userId as partition key
        verify(kafkaTemplate).send(eq("reports.generated"), eq(userId), eq(eventWithSpecificUser));
    }

    @Test
    void shouldPublishEventWithAllFields() {
        // Given
        UUID reportId = UUID.randomUUID();
        UUID activityId = UUID.randomUUID();
        Instant generatedAt = Instant.parse("2024-12-08T12:00:00Z");

        ReportGeneratedEventDto completeEvent = ReportGeneratedEventDto.builder()
                .reportId(reportId)
                .userId("complete-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{\"totalKm\": 52.0, \"trend\": \"improving\"}")
                .generatedAt(generatedAt)
                .triggerActivityId(activityId)
                .athleteName("Complete Runner")
                .currentGoal("Sub-3h marathon")
                .build();

        // When
        producer.publish(completeEvent);

        // Then
        verify(kafkaTemplate).send(
                eq("reports.generated"),
                eq("complete-user"),
                eq(completeEvent)
        );
    }

    @Test
    void shouldPublishMultipleEvents() {
        // Given
        ReportGeneratedEventDto event1 = ReportGeneratedEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("user-1")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .generatedAt(Instant.now())
                .athleteName("Runner One")
                .currentGoal("5k PR")
                .build();

        ReportGeneratedEventDto event2 = ReportGeneratedEventDto.builder()
                .reportId(UUID.randomUUID())
                .userId("user-2")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{}")
                .generatedAt(Instant.now())
                .athleteName("Runner Two")
                .currentGoal("10k PR")
                .build();

        // When
        producer.publish(event1);
        producer.publish(event2);

        // Then
        verify(kafkaTemplate).send(eq("reports.generated"), eq("user-1"), eq(event1));
        verify(kafkaTemplate).send(eq("reports.generated"), eq("user-2"), eq(event2));
    }
}
