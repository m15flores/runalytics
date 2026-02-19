package com.runalitycs.normalizer.kafka;

import com.runalitycs.normalizer.dto.ActivityNormalizedDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NormalizerProducer {

    private final KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate;

    @Value("${runalytics.kafka.topics.normalized}")
    private String normalizedTopic;

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