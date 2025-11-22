package com.runalitycs.activity.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ActivityProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public ActivityProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishActivity(String payload) {
        kafkaTemplate.send("raw-activities", payload);
    }
}
