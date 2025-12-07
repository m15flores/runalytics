package com.runalitycs.metrics_engine.kafka;

import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class MetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricsConsumer.class);

    private final MetricsService metricsService;

    public MetricsConsumer(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(
            topics = "activities.normalized",
            groupId = "metrics-engine-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ActivityNormalizedDto message,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key,
                       org.springframework.kafka.support.Acknowledgment acknowledgment) {
        log.info("Received activity from Kafka: {}", message.activityId());

        try {
            metricsService.processActivity(message);
            log.info("✅ Mensaje procesado exitosamente: {}", key);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Failed to process activity: {}", message.activityId(), e);
        }
    }
}