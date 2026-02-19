package com.runalitycs.normalizer.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalitycs.normalizer.dto.ActivityNormalizedDto;
import com.runalitycs.normalizer.dto.ActivitySample;
import com.runalitycs.normalizer.dto.ParsedFitData;
import com.runalitycs.normalizer.service.ActivityNormalizerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ActivityConsumer {

    private final ActivityNormalizerService normalizerService;
    private final NormalizerProducer normalizerProducer;
    private final ObjectMapper objectMapper;

    public ActivityConsumer(
            ActivityNormalizerService normalizerService,
            NormalizerProducer normalizerProducer
    ) {
        this.normalizerService = normalizerService;
        this.normalizerProducer = normalizerProducer;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @KafkaListener(
            topics = "${runalytics.kafka.topics.raw-ingested}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String message) {
        log.info("Received message from activities.raw.ingested");

        try {
            JsonNode json = objectMapper.readTree(message);

            String userId = json.get("userId").asText();
            String device = json.has("device") && !json.get("device").isNull()
                    ? json.get("device").asText()
                    : null;
            Instant timestamp = parseTimestamp(json.get("timestamp"));

            // Extract raw fields from the message payload
            JsonNode raw = json.get("raw");
            Integer durationSeconds = raw.has("duration_s")
                    ? raw.get("duration_s").asInt()
                    : 0;
            BigDecimal distanceMeters = raw.has("distance_m")
                    ? new BigDecimal(raw.get("distance_m").asText())
                    : BigDecimal.ZERO;

            List<ActivitySample> samples = extractSamples(raw);

            ParsedFitData parsedData = new ParsedFitData(
                    timestamp,
                    durationSeconds,
                    distanceMeters,
                    samples
            );

            log.info("Normalizing activity for user: {}", userId);

            ActivityNormalizedDto normalizedDto = normalizerService.normalize(
                    userId, device, parsedData
            );

            normalizerProducer.publish(normalizedDto);

            log.info("Activity normalized and published for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing message from activities.raw.ingested", e);
        }
    }

    private List<ActivitySample> extractSamples(JsonNode raw) {
        List<ActivitySample> samples = new ArrayList<>();

        if (raw.has("samples") && raw.get("samples").isArray()) {
            for (JsonNode sampleNode : raw.get("samples")) {
                Instant ts = sampleNode.has("ts")
                        ? Instant.parse(sampleNode.get("ts").asText())
                        : null;
                Double lat = sampleNode.has("lat")
                        ? sampleNode.get("lat").asDouble()
                        : null;
                Double lon = sampleNode.has("lon")
                        ? sampleNode.get("lon").asDouble()
                        : null;
                Integer hr = sampleNode.has("hr")
                        ? sampleNode.get("hr").asInt()
                        : null;
                Integer pace = sampleNode.has("pace")
                        ? sampleNode.get("pace").asInt()
                        : null;
                Double altitude = sampleNode.has("altitude")
                        ? sampleNode.get("altitude").asDouble()
                        : null;
                Integer cadence = sampleNode.has("cadence")
                        ? sampleNode.get("cadence").asInt()
                        : null;

                if (ts != null) {
                    samples.add(new ActivitySample(ts, lat, lon, hr, pace, altitude, cadence));
                }
            }
        }

        return samples;
    }

    private Instant parseTimestamp(JsonNode timestampNode) {
        if (timestampNode.isNumber()) {
            // Numeric timestamp: detect milliseconds vs seconds by magnitude
            long value = timestampNode.asLong();
            if (value > 1_000_000_000_000L) {
                return Instant.ofEpochMilli(value);
            } else {
                return Instant.ofEpochSecond(value);
            }
        } else if (timestampNode.isTextual()) {
            // String timestamp: parse as ISO-8601
            return Instant.parse(timestampNode.asText());
        } else {
            throw new IllegalArgumentException("Invalid timestamp format: " + timestampNode);
        }
    }
}