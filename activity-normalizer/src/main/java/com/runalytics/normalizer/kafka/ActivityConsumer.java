package com.runalytics.normalizer.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.normalizer.dto.ActivityNormalizedDto;
import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import com.runalytics.normalizer.service.ActivityNormalizerService;
import com.runalytics.normalizer.service.FitParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityConsumer {

    private final ActivityNormalizerService normalizerService;
    private final NormalizerProducer normalizerProducer;
    private final ObjectMapper objectMapper;
    private final FitParserService fitParserService;

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

            JsonNode raw = json.get("raw");
            ParsedFitData parsedData;
            if (raw != null && raw.has("fitBase64")) {
                log.info("action=consume type=fit userId={}", userId);
                byte[] fitBytes = Base64.getDecoder().decode(raw.get("fitBase64").asText());
                parsedData = fitParserService.parse(new ByteArrayInputStream(fitBytes));
            } else {
                log.info("action=consume type=json userId={}", userId);
                parsedData = buildParsedFitData(json);
            }

            log.info("Normalizing activity for user: {}", userId);

            ActivityNormalizedDto normalizedDto = normalizerService.normalize(userId, device, parsedData);
            normalizerProducer.publish(normalizedDto);

            log.info("Activity normalized and published for user: {}", userId);

        } catch (Exception e) {
            log.error("Error processing message from activities.raw.ingested", e);
        }
    }

    private ParsedFitData buildParsedFitData(JsonNode json) {
        Instant timestamp = parseTimestamp(json.get("timestamp"));

        // Extract raw fields from the message payload
        JsonNode raw = json.get("raw");
        Integer durationSeconds = raw.has("duration_s")
                ? raw.get("duration_s").asInt()
                : 0;
        BigDecimal distanceMeters = raw.has("distance_m")
                ? new BigDecimal(raw.get("distance_m").asText())
                : BigDecimal.ZERO;

        // Build a minimal SessionInfo from the available top-level fields
        ParsedFitData.SessionInfo sessionInfo = new ParsedFitData.SessionInfo(
                distanceMeters, durationSeconds, durationSeconds, null,
                null, null, null, null,
                null, null,
                null, null, null,
                null, null, null, null,
                null, null,
                null, null, null,
                null, null,
                null, null,
                null, null, null, null
        );

        return new ParsedFitData(timestamp, durationSeconds, distanceMeters, sessionInfo, List.of(), extractSamples(raw));
    }

    private List<ActivitySample> extractSamples(JsonNode raw) {
        List<ActivitySample> samples = new ArrayList<>();

        if (raw.has("samples") && raw.get("samples").isArray()) {
            for (JsonNode sampleNode : raw.get("samples")) {
                Instant ts = sampleNode.has("ts")
                        ? Instant.parse(sampleNode.get("ts").asText())
                        : null;
                Double lat = sampleNode.has("lat") ? sampleNode.get("lat").asDouble() : null;
                Double lon = sampleNode.has("lon") ? sampleNode.get("lon").asDouble() : null;
                Integer hr = sampleNode.has("hr") ? sampleNode.get("hr").asInt() : null;
                Integer pace = sampleNode.has("pace") ? sampleNode.get("pace").asInt() : null;
                Double altitude = sampleNode.has("altitude") ? sampleNode.get("altitude").asDouble() : null;
                Integer cadence = sampleNode.has("cadence") ? sampleNode.get("cadence").asInt() : null;

                if (ts != null) {
                    samples.add(new ActivitySample(ts, lat, lon, hr, pace, altitude, cadence, null, null, null));
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