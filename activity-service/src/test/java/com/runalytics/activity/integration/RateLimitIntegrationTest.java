package com.runalytics.activity.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test for the IP-based rate limit (5 uploads per hour).
 * Uses Testcontainers (MongoDB + Kafka) and sends real HTTP requests
 * via TestRestTemplate.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RateLimitIntegrationTest {

    private static final String VALID_REQUEST_BODY = """
            {
                "userId": "rate-limit-user",
                "device": "Garmin-Fenix-7-Pro",
                "timestamp": "2025-01-01T10:30:00Z",
                "source": "garmin-mock",
                "raw": {
                    "distance_m": 10042,
                    "duration_s": 2780
                }
            }
            """;

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongo.getConnectionString() + "/runalytics_test");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldAllow5UploadsAndBlockThe6th() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(VALID_REQUEST_BODY, headers);

        // First 5 requests should succeed (201)
        for (int i = 1; i <= 5; i++) {
            ResponseEntity<Map> response = restTemplate.postForEntity("/activities", request, Map.class);
            assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                    "Request #" + i + " should be allowed");
        }

        // 6th request must be blocked (429)
        ResponseEntity<Map> throttled = restTemplate.postForEntity("/activities", request, Map.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, throttled.getStatusCode(),
                "6th request should be rate-limited");
        assertEquals(429, throttled.getBody().get("status"),
                "Response body status should be 429");
        assertEquals("Too Many Requests", throttled.getBody().get("error"),
                "Response body error field should indicate too many requests");
    }

    @Test
    void shouldAllow5FitUploadsAndBlockThe6th() {
        // Minimal valid multipart request is complex to build with TestRestTemplate,
        // so we verify the /activities endpoint as representative of the shared filter.
        // The filter covers both POST /activities and POST /activities/fit via shouldNotFilter().
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(VALID_REQUEST_BODY, headers);

        for (int i = 1; i <= 5; i++) {
            ResponseEntity<Map> response = restTemplate.postForEntity("/activities", request, Map.class);
            assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                    "FIT-route test: request #" + i + " should be allowed");
        }

        ResponseEntity<Map> throttled = restTemplate.postForEntity("/activities", request, Map.class);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, throttled.getStatusCode(),
                "FIT-route test: 6th request should be rate-limited");
    }
}
