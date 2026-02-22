package com.runalytics.metrics_engine.kafka;

import com.runalytics.metrics_engine.dto.ActivityNormalizedDto;
import com.runalytics.metrics_engine.service.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsConsumer {

    private final MetricsService metricsService;

    @KafkaListener(
            topics = "${runalytics.kafka.topics.normalized}",
            groupId = "metrics-engine-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ActivityNormalizedDto message,
                        @Header(KafkaHeaders.RECEIVED_KEY) String key,
                        org.springframework.kafka.support.Acknowledgment acknowledgment) {
        log.info("Received activity from Kafka: {}", message.activityId());

        try {
            validateActivity(message);
            metricsService.processActivity(message);
            log.info("Message processed successfully: {}", key);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // Validation errors: do not retry
            log.error("Validation error (will not retry): {}", key, e);
            acknowledgment.acknowledge();

        } catch (DataAccessException e) {
            // DB error: retry (may be transient)
            log.error("DB error (will retry): {}", key, e);
            throw e;

        } catch (Exception e) {
            // Unknown error: log and retry
            log.error("Unexpected error processing activity: {}", key, e);
            throw e;
        }
    }

    /** Basic message validation */
    private void validateActivity(ActivityNormalizedDto activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }
        if (activity.activityId() == null) {
            throw new IllegalArgumentException("ActivityId cannot be null");
        }
        if (activity.userId() == null || activity.userId().isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
        if (activity.session() == null) {
            throw new IllegalArgumentException("SessionData cannot be null");
        }
        log.debug("Basic validation passed for activity: {}", activity.activityId());
    }
}