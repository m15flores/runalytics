package com.runalytics.metrics_engine.config;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.function.BiFunction;

@Slf4j
@Configuration
public class KafkaErrorHandlingConfig {

    @Value("${runalytics.kafka.topics.normalized-dlq}")
    private String dlqTopic;

    /**
     * Error handler with Dead Letter Queue (DLQ).
     * Strategy:
     * 1. Retry 3 times with 2-second backoff
     * 2. On continued failure, send to DLQ
     * 3. Non-retryable exceptions (validation) go directly to DLQ
     */
    @Bean
    public CommonErrorHandler errorHandler(
            KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate
    ) {
        log.info("Configuring error handler with DLQ: {}", dlqTopic);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                destinationResolver()
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2000L, 3L)
        );

        // Exceptions that must NOT be retried (go directly to DLQ)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );

        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retry attempt {} for record key={} error={}",
                        deliveryAttempt, record.key(), ex.getMessage())
        );

        return errorHandler;
    }

    /** Resolves which DLQ topic partition to send a failed message to. */
    private BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver() {
        return (record, ex) -> {
            log.error("Sending message to DLQ: topic={} key={} error={}",
                    record.topic(), record.key(), ex.getMessage());
            return new TopicPartition(dlqTopic, 0);
        };
    }
}