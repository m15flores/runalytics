package com.runalitycs.activity.kafka;

import com.runalitycs.activity.dto.ActivityDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class ActivityProducer {

    private static final Logger log = LoggerFactory.getLogger(ActivityProducer.class);

    private final KafkaTemplate<String, ActivityDto> kafkaTemplate;

    @Value("${runalytics.kafka.topics.raw-ingested}")
    private String rawIngestedTopic;

    public ActivityProducer(KafkaTemplate<String, ActivityDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishActivity(ActivityDto dto) {
        log.debug("Publishing activity for user: {} to topic: {}", dto.userId(), rawIngestedTopic);

        CompletableFuture<SendResult<String, ActivityDto>> future =
                kafkaTemplate.send(rawIngestedTopic, dto.userId(), dto);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish activity for user: {}", dto.userId(), ex);
            } else {
                log.info("Activity published successfully for user: {} to partition: {}",
                        dto.userId(), result.getRecordMetadata().partition());
            }
        });
    }
}
