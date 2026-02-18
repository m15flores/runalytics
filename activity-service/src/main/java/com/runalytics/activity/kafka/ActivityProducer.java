package com.runalytics.activity.kafka;

import com.runalytics.activity.dto.ActivityDto;
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
public class ActivityProducer {

    private final KafkaTemplate<String, ActivityDto> kafkaTemplate;

    @Value("${runalytics.kafka.topics.raw-ingested}")
    private String rawIngestedTopic;

    public void publishActivity(ActivityDto dto) {
        log.info("action=publish userId={} topic={}", dto.userId(), rawIngestedTopic);

        CompletableFuture<SendResult<String, ActivityDto>> future =
                kafkaTemplate.send(rawIngestedTopic, dto.userId(), dto);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("action=publish status=error userId={}", dto.userId(), ex);
            } else {
                log.info("action=publish status=success userId={} partition={}",
                        dto.userId(), result.getRecordMetadata().partition());
            }
        });
    }
}