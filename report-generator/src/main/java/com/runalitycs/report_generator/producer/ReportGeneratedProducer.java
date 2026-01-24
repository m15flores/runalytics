package com.runalitycs.report_generator.producer;

import com.runalitycs.report_generator.dto.ReportGeneratedEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportGeneratedProducer {

    private final KafkaTemplate<String, ReportGeneratedEventDto> kafkaTemplate;

    @Value("${app.kafka.topics.reports-generated}")
    private String topic;

    public void publish(ReportGeneratedEventDto event) {
        log.info("Publishing report generated event: reportId={}, userId={}",
                event.reportId(), event.userId());

        kafkaTemplate.send(topic, event.userId(), event);
    }
}