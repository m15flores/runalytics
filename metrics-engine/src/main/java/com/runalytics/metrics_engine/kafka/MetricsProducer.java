package com.runalytics.metrics_engine.kafka;

import com.runalytics.metrics_engine.dto.ActivityMetricsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsProducer {

    @Value("${runalytics.kafka.topics.metrics-calculated}")
    private String topic;

    private final KafkaTemplate<String, ActivityMetricsDto> kafkaTemplate;

    public void publishMetrics(ActivityMetricsDto metrics) {
        String key = metrics.activityId().toString();

        log.info("Publishing metrics for activity: {}", key);

        kafkaTemplate.send(topic, key, metrics)
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