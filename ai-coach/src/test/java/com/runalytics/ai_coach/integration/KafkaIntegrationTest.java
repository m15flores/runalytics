package com.runalytics.ai_coach.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.TrainingVerdict;
import com.runalytics.ai_coach.dto.RecommendationGeneratedEventDto;
import com.runalytics.ai_coach.dto.TrainingReportEventDto;
import com.runalytics.ai_coach.repository.RecommendationRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
        partitions = 1,
        topics = {"reports.generated", "recommendations.generated"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:19092",
                "port=19092"
        }
)
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=localhost:19092",
        "app.kafka.topics.reports-generated=reports.generated",
        "app.kafka.topics.recommendations-generated=recommendations.generated",
        "openai.api.key=test-key",
        "spring.main.allow-bean-definition-overriding=true"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class KafkaIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static WireMockServer wireMockServer;

    static {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @Autowired
    private KafkaTemplate<String, TrainingReportEventDto> reportProducerTemplate;

    @Autowired
    private RecommendationRepository recommendationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private KafkaMessageListenerContainer<String, RecommendationGeneratedEventDto> container;
    private BlockingQueue<ConsumerRecord<String, RecommendationGeneratedEventDto>> records;

    /**
     * Test configuration to mock OpenAI WebClient
     */
    @TestConfiguration
    @EnableKafka
    static class TestConfig {

        @Bean
        @Primary
        public WebClient openAiWebClient() {
            // Point to WireMock server with dynamic port
            return WebClient.builder()
                    .baseUrl("http://localhost:" + wireMockServer.port())
                    .defaultHeader("Authorization", "Bearer test-key")
                    .defaultHeader("Content-Type", "application/json")
                    .build();
        }

        @Bean
        public KafkaTemplate<String, TrainingReportEventDto> reportProducerTemplate(
                EmbeddedKafkaBroker embeddedKafka) {

            Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

            ProducerFactory<String, TrainingReportEventDto> pf = new DefaultKafkaProducerFactory<>(producerProps);
            return new KafkaTemplate<>(pf);
        }

        @Bean
        @Primary
        public KafkaTemplate<String, RecommendationGeneratedEventDto> recommendationKafkaTemplate(
                EmbeddedKafkaBroker embeddedKafka) {

            Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

            ProducerFactory<String, RecommendationGeneratedEventDto> pf = new DefaultKafkaProducerFactory<>(producerProps);
            return new KafkaTemplate<>(pf);
        }
    }

    @BeforeEach
    void setUp() {
        // Clear database
        recommendationRepository.deleteAll();

        // Reset WireMock stubs for each test
        wireMockServer.resetAll();
        WireMock.configureFor("localhost", wireMockServer.port());

        // Setup consumer for recommendations.generated topic with unique group ID
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                "test-group-" + UUID.randomUUID(),
                "true",
                embeddedKafka
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RecommendationGeneratedEventDto.class.getName());

        DefaultKafkaConsumerFactory<String, RecommendationGeneratedEventDto> cf =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("recommendations.generated");
        container = new KafkaMessageListenerContainer<>(cf, containerProperties);

        records = new LinkedBlockingQueue<>();
        container.setupMessageListener((MessageListener<String, RecommendationGeneratedEventDto>) records::add);
        container.start();

        ContainerTestUtils.waitForAssignment(container, embeddedKafka.getPartitionsPerTopic());
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void shouldConsumeReportAndProduceRecommendations() throws Exception {
        // Given - Mock OpenAI response
        String mockAiResponse = """
                {
                  "id": "chatcmpl-test",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"verdict\\": \\"PARTIALLY_VALID\\", \\"verdict_rationale\\": \\"Z2 work drifted into Z3\\", \\"recommendations\\": [{\\"type\\": \\"ZONE_COMPLIANCE\\", \\"priority\\": \\"HIGH\\", \\"category\\": \\"ADJUSTMENT\\", \\"content\\": \\"Reduce pace by 15-20 sec/km on Z2 runs\\", \\"rationale\\": \\"Current pace pushes HR into Z3\\"}]}"
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {
                    "prompt_tokens": 100,
                    "completion_tokens": 50,
                    "total_tokens": 150
                  }
                }
                """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockAiResponse)));

        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .summaryJson("{\"totalKm\": 52.5, \"averagePace\": 305}")
                .generatedAt(Instant.now())
                .build();

        System.out.println("=== INITIAL STATE ===");
        System.out.println("Recommendations in DB: " + recommendationRepository.count());

        // When - Publish event to reports.generated
        reportProducerTemplate.send("reports.generated", "test-user", event);

        System.out.println("=== EVENT PUBLISHED ===");

        // Then - Wait for recommendations to be saved to DB
        await()
                .atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long count = recommendationRepository.count();
                    System.out.println("Recommendations in DB: " + count);
                    assertThat(count).isGreaterThan(0);
                });

        // Verify recommendations in database
        List<Recommendation> recommendations = recommendationRepository.findAll();
        assertThat(recommendations).hasSize(1);

        Recommendation rec = recommendations.get(0);
        assertThat(rec.getUserId()).isEqualTo("test-user");
        assertThat(rec.getReportId()).isEqualTo(reportId);
        assertThat(rec.getType().name()).isEqualTo("ZONE_COMPLIANCE");
        assertThat(rec.getPriority().name()).isEqualTo("HIGH");
        assertThat(rec.getVerdict()).isEqualTo(TrainingVerdict.PARTIALLY_VALID);
        assertThat(rec.getWeekInCycle()).isNotNull();

        System.out.println("=== RECOMMENDATION DETAILS ===");
        System.out.println("Type: " + rec.getType());
        System.out.println("Priority: " + rec.getPriority());
        System.out.println("Content: " + rec.getContent());
        System.out.println("Verdict: " + rec.getVerdict());

        // Wait for Kafka event on recommendations.generated
        ConsumerRecord<String, RecommendationGeneratedEventDto> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();

        RecommendationGeneratedEventDto publishedEvent = record.value();
        assertThat(publishedEvent.getReportId()).isEqualTo(reportId);
        assertThat(publishedEvent.getUserId()).isEqualTo("test-user");
        assertThat(publishedEvent.getVerdict()).isEqualTo(TrainingVerdict.PARTIALLY_VALID);
        assertThat(publishedEvent.getRecommendations()).hasSize(1);

        System.out.println("=== KAFKA EVENT PUBLISHED ===");
        System.out.println("Event userId: " + publishedEvent.getUserId());
        System.out.println("Event verdict: " + publishedEvent.getVerdict());
        System.out.println("Recommendations count: " + publishedEvent.getRecommendations().size());

        // Verify OpenAI was called
        verify(postRequestedFor(urlEqualTo("/chat/completions")));
    }

    @Test
    void shouldHandleMultipleRecommendations() throws Exception {
        // Given - Mock OpenAI response with multiple recommendations
        String mockAiResponse = """
                {
                  "id": "chatcmpl-multi",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"verdict\\": \\"INVALID\\", \\"verdict_rationale\\": \\"Multiple zone violations\\", \\"recommendations\\": [{\\"type\\": \\"ZONE_COMPLIANCE\\", \\"priority\\": \\"HIGH\\", \\"category\\": \\"RISK\\", \\"content\\": \\"Stop running in Z3 when Z2 prescribed\\", \\"rationale\\": \\"Ego-driven pace\\"},{\\"type\\": \\"CARDIAC_DRIFT\\", \\"priority\\": \\"HIGH\\", \\"category\\": \\"RISK\\", \\"content\\": \\"HR drift exceeds 5%% threshold\\", \\"rationale\\": \\"Aerobic inefficiency\\"},{\\"type\\": \\"RECOVERY\\", \\"priority\\": \\"MEDIUM\\", \\"category\\": \\"ADJUSTMENT\\", \\"content\\": \\"Add extra rest day\\", \\"rationale\\": \\"Accumulated fatigue\\"}]}"
                    },
                    "finish_reason": "stop"
                  }]
                }
                """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockAiResponse)));

        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user-2")
                .weekNumber(50)
                .year(2024)
                .summaryJson("{\"totalKm\": 65.0}")
                .generatedAt(Instant.now())
                .build();

        // When
        reportProducerTemplate.send("reports.generated", "test-user-2", event);

        // Then - Wait for all recommendations
        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(recommendationRepository.count()).isEqualTo(3);
                });

        List<Recommendation> recommendations = recommendationRepository.findByReportId(reportId);
        assertThat(recommendations).hasSize(3);

        // Verify verdict is set on all recommendations
        assertThat(recommendations)
                .extracting(Recommendation::getVerdict)
                .containsOnly(TrainingVerdict.INVALID);

        // Verify priorities
        assertThat(recommendations)
                .filteredOn(r -> r.getPriority().name().equals("HIGH"))
                .hasSize(2);

        // Verify Kafka event
        ConsumerRecord<String, RecommendationGeneratedEventDto> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getRecommendations()).hasSize(3);
    }

    @Test
    void shouldHandleValidTrainingWithConfirmations() throws Exception {
        // Given - Mock OpenAI response for valid training
        String mockAiResponse = """
                {
                  "id": "chatcmpl-valid",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"verdict\\": \\"VALID\\", \\"verdict_rationale\\": \\"Excellent Z2 compliance and progression\\", \\"recommendations\\": [{\\"type\\": \\"GOAL_PROGRESS\\", \\"priority\\": \\"LOW\\", \\"category\\": \\"CONFIRMATION\\", \\"content\\": \\"On track for marathon goal\\", \\"rationale\\": \\"Volume and pace progressing well\\"},{\\"type\\": \\"TRAINING_VOLUME\\", \\"priority\\": \\"LOW\\", \\"category\\": \\"CONFIRMATION\\", \\"content\\": \\"Safe 8%% volume increase\\", \\"rationale\\": \\"Below 10%% guideline\\"}]}"
                    },
                    "finish_reason": "stop"
                  }]
                }
                """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockAiResponse)));

        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("good-athlete")
                .weekNumber(10)
                .year(2024)
                .summaryJson("{\"totalKm\": 54.0}")
                .generatedAt(Instant.now())
                .build();

        // When
        reportProducerTemplate.send("reports.generated", "good-athlete", event);

        // Then
        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(recommendationRepository.count()).isEqualTo(2);
                });

        List<Recommendation> recommendations = recommendationRepository.findAll();

        // Verify VALID verdict
        assertThat(recommendations)
                .extracting(Recommendation::getVerdict)
                .containsOnly(TrainingVerdict.VALID);

        // Verify LOW priority confirmations
        assertThat(recommendations)
                .extracting(Recommendation::getPriority)
                .containsOnly(Priority.LOW);

        // Verify Kafka event
        ConsumerRecord<String, RecommendationGeneratedEventDto> record = records.poll(10, TimeUnit.SECONDS);
        assertThat(record).isNotNull();
        assertThat(record.value().getVerdict()).isEqualTo(TrainingVerdict.VALID);
    }

    @Test
    void shouldSetWeekInCycleCorrectly() throws Exception {
        // Given - Week 12 = cycle 3, week 4 (deload)
        String mockAiResponse = """
                {
                  "id": "chatcmpl-deload",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"verdict\\": \\"VALID\\", \\"verdict_rationale\\": \\"Perfect deload execution\\", \\"recommendations\\": [{\\"type\\": \\"RECOVERY\\", \\"priority\\": \\"MEDIUM\\", \\"category\\": \\"CONFIRMATION\\", \\"content\\": \\"Deload executed correctly\\", \\"rationale\\": \\"Volume reduced appropriately\\"}]}"
                    },
                    "finish_reason": "stop"
                  }]
                }
                """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockAiResponse)));

        UUID reportId = UUID.randomUUID();
        TrainingReportEventDto event = TrainingReportEventDto.builder()
                .reportId(reportId)
                .userId("test-user-3")
                .weekNumber(12) // Week 4 of cycle 3
                .year(2024)
                .summaryJson("{\"totalKm\": 30.0}")
                .generatedAt(Instant.now())
                .build();

        // When
        reportProducerTemplate.send("reports.generated", "test-user-3", event);

        // Then
        await()
                .atMost(15, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    List<Recommendation> recs = recommendationRepository.findAll();
                    assertThat(recs).hasSize(1);

                    Recommendation rec = recs.get(0);
                    assertThat(rec.getWeekInCycle()).isEqualTo(4);
                    assertThat(rec.getTrainingPhase()).isEqualTo(TrainingCycleContext.TrainingPhase.AEROBIC_BASE);
                });
    }
}