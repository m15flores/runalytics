package com.runalytics.metrics_engine.kafka;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetricsProducer {

    private static final Logger log = LoggerFactory.getLogger(MetricsProducer.class);
    private static final String TOPIC = "activities.metrics.calculated";

    private final KafkaTemplate<String, ActivityMetricsDto> kafkaTemplate;

    public MetricsProducer(KafkaTemplate<String, ActivityMetricsDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica métricas calculadas a Kafka.
     *
     * @param metrics métricas de la actividad
     */
    public void publishMetrics(ActivityMetricsDto metrics) {
        String key = metrics.activityId().toString();

        log.info("Publishing metrics for activity: {}", key);

        kafkaTemplate.send(TOPIC, key, metrics)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish metrics for activity: {}", key, ex);
                    } else {
                        log.info("Successfully published metrics for activity: {} to partition: {}",
                                key, result.getRecordMetadata().partition());
                    }
                });
    }
}