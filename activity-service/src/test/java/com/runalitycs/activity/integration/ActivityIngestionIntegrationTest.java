package com.runalitycs.activity.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ActivityIngestionIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldIngestActivityAndPublishToKafka() throws Exception {
        // Given
        String requestBody = """
        {
            "userId": "user-12345",
            "device": "Garmin-Fenix-7-Pro",
            "timestamp": "2025-01-01T10:30:00Z",
            "source": "garmin-mock",
            "raw": {
                "distance_m": 10042,
                "duration_s": 2780
            }
        }
        """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // When
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/activities",
                request,  // ✅ Usa HttpEntity, no String directo
                Map.class
        );

        // Then - Verificar respuesta HTTP
        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("user-12345", response.getBody().get("userId"));

        // Then - Verificar mensaje en Kafka
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("activities.raw.ingested"));

        // Poll mensajes (esperar hasta 10 segundos)
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(10));

        // Verificar que recibimos al menos 1 mensaje
        assertFalse(records.isEmpty(), "Should have received a message in Kafka");

        ConsumerRecord<String, String> record = records.iterator().next();

        // Parsear JSON del mensaje y verificar contenido
        ObjectMapper mapper = new ObjectMapper();
        JsonNode json = mapper.readTree(record.value());

        assertEquals("user-12345", json.get("userId").asText());
        assertEquals("Garmin-Fenix-7-Pro", json.get("device").asText());
        assertEquals("garmin-mock", json.get("source").asText());
        assertNotNull(json.get("raw"));
        assertEquals(10042, json.get("raw").get("distance_m").asInt());
        assertEquals(2780, json.get("raw").get("duration_s").asInt());

        consumer.close();
    }
}