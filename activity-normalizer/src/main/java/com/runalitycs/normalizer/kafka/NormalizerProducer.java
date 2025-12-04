package com.runalitycs.normalizer.kafka;

import com.runalitycs.normalizer.dto.ActivityNormalizedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class NormalizerProducer {

    private static final Logger log = LoggerFactory.getLogger(NormalizerProducer.class);

    private final KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Value("${runalytics.kafka.topics.normalized}")
    private String normalizedTopic;

    public NormalizerProducer(KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ActivityNormalizedDto dto) {
        log.debug("Publishing normalized activity for user: {} to topic: {}",
                dto.userId(), normalizedTopic);

        CompletableFuture<SendResult<String, ActivityNormalizedDto>> future =
                kafkaTemplate.send(normalizedTopic, dto.userId(), dto);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish normalized activity for user: {}", dto.userId(), ex);
            } else {
                log.info("Normalized activity published successfully for user: {} to partition: {}",
                        dto.userId(), result.getRecordMetadata().partition());
            }
        });
    }
}