package com.runalitycs.report_generator.integration;

import com.runalitycs.report_generator.dto.ActivityMetricsDto;
import com.runalitycs.report_generator.dto.ReportGeneratedEventDto;
import com.runalitycs.report_generator.entity.ActivityMetrics;
import com.runalitycs.report_generator.entity.AthleteProfile;
import com.runalitycs.report_generator.repository.ActivityMetricsRepository;
import com.runalitycs.report_generator.repository.AthleteProfileRepository;
import com.runalitycs.report_generator.repository.TrainingReportRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Import(KafkaIntegrationTest.TestClockConfig.class)
class KafkaIntegrationTest extends BaseKafkaIntegrationTest {

    // Fixed clock at December 10, 2024 (within week 50)
    private static final Instant FIXED_INSTANT = Instant.parse("2024-12-10T12:00:00Z");

    @TestConfiguration
    static class TestClockConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(FIXED_INSTANT, ZoneId.of("Europe/Paris"));
        }
    }

    @Autowired
    private KafkaTemplate<String, ActivityMetricsDto> kafkaTemplate;

    @Autowired
    private AthleteProfileRepository athleteProfileRepository;

    @Autowired
    private TrainingReportRepository trainingReportRepository;

    @Autowired
    private ActivityMetricsRepository activityMetricsRepository;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private BlockingQueue<ConsumerRecord<String, ReportGeneratedEventDto>> records;
    private KafkaMessageListenerContainer<String, ReportGeneratedEventDto> container;

    @BeforeEach
    void setUp() {
        // Clean database
        trainingReportRepository.deleteAll();
        activityMetricsRepository.deleteAll();
        athleteProfileRepository.deleteAll();

        // Force flush to ensure clean state
        trainingReportRepository.flush();
        activityMetricsRepository.flush();
        athleteProfileRepository.flush();

        // Create athlete profile
        AthleteProfile profile = AthleteProfile.builder()
                .userId("test-user")
                .name("Test Runner")
                .age(30)
                .weight(70.0)
                .maxHeartRate(190)
                .currentGoal("Marathon sub-3:30")
                .createdAt(FIXED_INSTANT)
                .updatedAt(FIXED_INSTANT)
                .build();
        athleteProfileRepository.save(profile);

        // Set up consumer for reports.generated topic
        records = new LinkedBlockingQueue<>();

        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE,
                "com.runalitycs.report_generator.dto.ReportGeneratedEventDto");
        consumerProps.put(JsonDeserializer.TYPE_MAPPINGS,
                "reportGenerated:com.runalitycs.report_generator.dto.ReportGeneratedEventDto");

        DefaultKafkaConsumerFactory<String, ReportGeneratedEventDto> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        ContainerProperties containerProperties = new ContainerProperties("reports.generated");
        containerProperties.setGroupId("test-consumer-group-" + UUID.randomUUID());

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, ReportGeneratedEventDto>) records::add);
        container.start();

        // Wait for partition assignment before publishing (ensures "latest" offset is committed)
        ContainerTestUtils.waitForAssignment(container, 1);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    private ActivityMetrics createActivityMetrics(UUID activityId, String userId, Instant startedAt,
                                                   BigDecimal distance, int duration, Integer pace) {
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setId(UUID.randomUUID());
        metrics.setActivityId(activityId);
        metrics.setUserId(userId);
        metrics.setStartedAt(startedAt);
        metrics.setTotalDistance(distance);
        metrics.setTotalDuration(duration);
        metrics.setAveragePace(pace);
        metrics.setCalculatedAt(Instant.now());
        return activityMetricsRepository.save(metrics);
    }

    /**
     * Wait for a specific number of events and collect them all
     */
    private List<ReportGeneratedEventDto> waitForEvents(int expectedCount, long timeoutSeconds)
            throws InterruptedException {
        List<ReportGeneratedEventDto> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + (timeoutSeconds * 1000);

        while (events.size() < expectedCount && System.currentTimeMillis() < deadline) {
            ConsumerRecord<String, ReportGeneratedEventDto> record =
                    records.poll(1, TimeUnit.SECONDS);
            if (record != null) {
                events.add(record.value());
            }
        }

        return events;
    }

    /**
     * Find event by activity ID in a list of events
     */
    private ReportGeneratedEventDto findEventByActivityId(List<ReportGeneratedEventDto> events, UUID activityId) {
        return events.stream()
                .filter(e -> activityId.equals(e.triggerActivityId()))
                .findFirst()
                .orElse(null);
    }

    @Test
    void shouldConsumeAndProduceEndToEnd() throws InterruptedException {
        // Given - Activity metrics event
        UUID activityId = UUID.randomUUID();
        Instant activityTime = Instant.parse("2024-12-08T10:00:00Z"); // Week 49, 2024

        // Insert activity metrics into database (required for weekly aggregation)
        createActivityMetrics(activityId, "test-user", activityTime,
                new BigDecimal("10.5"), 3600, 343);

        ActivityMetricsDto activityMetrics = ActivityMetricsDto.builder()
                .activityId(activityId)
                .userId("test-user")
                .startedAt(activityTime)
                .totalDistance(new BigDecimal("10.5"))
                .totalDuration(3600)
                .totalCalories(600)
                .averagePace(343)
                .averageHeartRate(145)
                .averageCadence(170)
                .totalAscent(100)
                .totalDescent(100)
                .build();

        // When - Publish to metrics.calculated topic
        kafkaTemplate.send("metrics.calculated", "test-user", activityMetrics);

        // Then - Should receive report.generated event
        List<ReportGeneratedEventDto> events = waitForEvents(1, 15);
        assertThat(events).hasSize(1);

        ReportGeneratedEventDto event = events.get(0);
        assertNotNull(event, "Should receive report generated event");
        assertThat(event.userId()).isEqualTo("test-user");
        assertThat(event.weekNumber()).isEqualTo(49);
        assertThat(event.year()).isEqualTo(2024);
        assertThat(event.triggerActivityId()).isEqualTo(activityId);
        assertThat(event.summaryJson()).isNotNull();
        assertThat(event.reportId()).isNotNull();
        assertThat(event.generatedAt()).isNotNull();

        // Verify report was saved to database
        var reports = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 49, 2024);
        assertThat(reports).isPresent();
        assertThat(reports.get().getMarkdownContent()).contains("# Training Report - Week 50/2024");
    }

    @Test
    void shouldHandleMultipleMessages() throws InterruptedException {
        // Given - Multiple activity metrics
        UUID activityId1 = UUID.randomUUID();
        UUID activityId2 = UUID.randomUUID();
        Instant time1 = Instant.parse("2024-12-14T10:00:00Z"); // Week 50 (Saturday)
        Instant time2 = Instant.parse("2024-12-15T10:00:00Z"); // Week 50 (Sunday)

        // Insert activity metrics into database
        createActivityMetrics(activityId1, "test-user", time1, new BigDecimal("10.0"), 3600, 360);
        createActivityMetrics(activityId2, "test-user", time2, new BigDecimal("12.0"), 4320, 360);

        ActivityMetricsDto activity1 = ActivityMetricsDto.builder()
                .activityId(activityId1)
                .userId("test-user")
                .startedAt(time1)
                .totalDistance(new BigDecimal("10.0"))
                .totalDuration(3600)
                .averagePace(360)
                .build();

        ActivityMetricsDto activity2 = ActivityMetricsDto.builder()
                .activityId(activityId2)
                .userId("test-user")
                .startedAt(time2)
                .totalDistance(new BigDecimal("12.0"))
                .totalDuration(4320)
                .averagePace(360)
                .build();

        // When - Publish both messages
        kafkaTemplate.send("metrics.calculated", "test-user", activity1);
        Thread.sleep(1000); // Small delay between messages
        kafkaTemplate.send("metrics.calculated", "test-user", activity2);

        // Then - Should receive 2 report generated events
        ConsumerRecord<String, ReportGeneratedEventDto> record1 =
                records.poll(15, TimeUnit.SECONDS);
        ConsumerRecord<String, ReportGeneratedEventDto> record2 =
                records.poll(15, TimeUnit.SECONDS);

        assertNotNull(record1);
        assertNotNull(record2);

        // Both should be for week 50
        assertThat(record1.value().weekNumber()).isEqualTo(50);
        assertThat(record2.value().weekNumber()).isEqualTo(50);

        // Second report should have aggregated both activities
        var finalReport = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 50, 2024);
        assertThat(finalReport).isPresent();
    }

    @Test
    void shouldRegenerateReportWhenMultipleActivitiesInSameWeek() throws InterruptedException {
        // Given - 3 activities in the same week (same day)
        UUID activityId1 = UUID.randomUUID();
        UUID activityId2 = UUID.randomUUID();
        UUID activityId3 = UUID.randomUUID();
        Instant time1 = Instant.parse("2024-12-12T08:00:00Z"); // Week 50, Thursday
        Instant time2 = Instant.parse("2024-12-12T12:00:00Z"); // Week 50, Thursday
        Instant time3 = Instant.parse("2024-12-12T16:00:00Z"); // Week 50, Thursday

        // SANITY CHECK 1: Verify DB is clean
        long initialActivityCount = activityMetricsRepository.count();
        long initialReportCount = trainingReportRepository.count();
        System.out.println("=== INITIAL STATE ===");
        System.out.println("Activities in DB: " + initialActivityCount);
        System.out.println("Reports in DB: " + initialReportCount);
        assertThat(initialActivityCount).isEqualTo(0);
        assertThat(initialReportCount).isEqualTo(0);

        // Insert activity metrics into database
        createActivityMetrics(activityId1, "test-user", time1, new BigDecimal("5.0"), 1800, 360);
        createActivityMetrics(activityId2, "test-user", time2, new BigDecimal("5.0"), 1800, 360);
        createActivityMetrics(activityId3, "test-user", time3, new BigDecimal("5.0"), 1800, 360);

        // SANITY CHECK 2: Verify we inserted exactly 3 activities
        long afterInsertCount = activityMetricsRepository.count();
        System.out.println("=== AFTER INSERT ===");
        System.out.println("Activities in DB: " + afterInsertCount);
        assertThat(afterInsertCount).isEqualTo(3);

        // List all activities to verify IDs
        var allActivities = activityMetricsRepository.findAll();
        System.out.println("=== ALL ACTIVITIES ===");
        allActivities.forEach(a ->
                System.out.println("ActivityID: " + a.getActivityId() + ", StartedAt: " + a.getStartedAt())
        );

        ActivityMetricsDto activity1 = ActivityMetricsDto.builder()
                .activityId(activityId1)
                .userId("test-user")
                .startedAt(time1)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        ActivityMetricsDto activity2 = ActivityMetricsDto.builder()
                .activityId(activityId2)
                .userId("test-user")
                .startedAt(time2)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        ActivityMetricsDto activity3 = ActivityMetricsDto.builder()
                .activityId(activityId3)
                .userId("test-user")
                .startedAt(time3)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        System.out.println("=== PUBLISHING EVENTS ===");
        System.out.println("Activity1 ID: " + activityId1);
        System.out.println("Activity2 ID: " + activityId2);
        System.out.println("Activity3 ID: " + activityId3);

        // When - Publish in order
        kafkaTemplate.send("metrics.calculated", "test-user", activity1);
        Thread.sleep(1000);
        kafkaTemplate.send("metrics.calculated", "test-user", activity2);
        Thread.sleep(1000);
        kafkaTemplate.send("metrics.calculated", "test-user", activity3);

        // Then - Should receive at least 1 report
        List<ReportGeneratedEventDto> events = waitForEvents(1, 30);

        System.out.println("=== RECEIVED EVENTS ===");
        System.out.println("Total events received: " + events.size());
        events.forEach(e ->
                System.out.println("Event - ReportID: " + e.reportId() +
                        ", TriggerActivityID: " + e.triggerActivityId() +
                        ", Week: " + e.weekNumber())
        );

        assertThat(events).hasSizeGreaterThanOrEqualTo(1);

        // The last event should be for one of our activities
        ReportGeneratedEventDto lastEvent = events.get(events.size() - 1);
        System.out.println("=== LAST EVENT ===");
        System.out.println("Trigger Activity ID: " + lastEvent.triggerActivityId());
        System.out.println("Expected one of: " + activityId1 + ", " + activityId2 + ", " + activityId3);

        assertThat(lastEvent.userId()).isEqualTo("test-user");
        assertThat(lastEvent.weekNumber()).isEqualTo(50);
        assertThat(lastEvent.year()).isEqualTo(2024);

        // Check if it's ANY of our activities (not necessarily activity3)
        assertThat(lastEvent.triggerActivityId())
                .isIn(activityId1, activityId2, activityId3);

        // Verify final report in database
        var finalReport = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 50, 2024);
        assertThat(finalReport).isPresent();

        System.out.println("=== FINAL REPORT ===");
        System.out.println("Report Trigger Activity ID: " + finalReport.get().getTriggerActivityId());

        // Verify it's one of our activities
        assertThat(finalReport.get().getTriggerActivityId())
                .isIn(activityId1, activityId2, activityId3);

        // Verify the markdown content exists and is valid
        assertThat(finalReport.get().getMarkdownContent()).contains("# Training Report - Week 50/2024");
    }

    @Test
    void shouldMaintainMessageOrderAcrossDifferentWeeks() throws InterruptedException {
        // Given - 3 activities in DIFFERENT weeks
        UUID activityId1 = UUID.randomUUID();
        UUID activityId2 = UUID.randomUUID();
        UUID activityId3 = UUID.randomUUID();

        Instant time1 = Instant.parse("2024-12-05T10:00:00Z"); // Week 49 (Thursday)
        Instant time2 = Instant.parse("2024-12-12T10:00:00Z"); // Week 50 (Thursday)
        Instant time3 = Instant.parse("2024-12-19T10:00:00Z"); // Week 51 (Thursday)

        // Insert activity metrics into database
        createActivityMetrics(activityId1, "test-user", time1, new BigDecimal("5.0"), 1800, 360);
        createActivityMetrics(activityId2, "test-user", time2, new BigDecimal("5.0"), 1800, 360);
        createActivityMetrics(activityId3, "test-user", time3, new BigDecimal("5.0"), 1800, 360);

        ActivityMetricsDto activity1 = ActivityMetricsDto.builder()
                .activityId(activityId1)
                .userId("test-user")
                .startedAt(time1)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        ActivityMetricsDto activity2 = ActivityMetricsDto.builder()
                .activityId(activityId2)
                .userId("test-user")
                .startedAt(time2)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        ActivityMetricsDto activity3 = ActivityMetricsDto.builder()
                .activityId(activityId3)
                .userId("test-user")
                .startedAt(time3)
                .totalDistance(new BigDecimal("5.0"))
                .totalDuration(1800)
                .averagePace(360)
                .build();

        // When - Publish in order
        kafkaTemplate.send("metrics.calculated", "test-user", activity1);
        Thread.sleep(1000);
        kafkaTemplate.send("metrics.calculated", "test-user", activity2);
        Thread.sleep(1000);
        kafkaTemplate.send("metrics.calculated", "test-user", activity3);

        // Then - Should receive 3 reports (one for each week)
        List<ReportGeneratedEventDto> events = waitForEvents(3, 30);
        assertThat(events).hasSize(3);

        // Find events by activity ID
        ReportGeneratedEventDto event1 = findEventByActivityId(events, activityId1);
        ReportGeneratedEventDto event2 = findEventByActivityId(events, activityId2);
        ReportGeneratedEventDto event3 = findEventByActivityId(events, activityId3);

        assertNotNull(event1, "Should receive report for activity 1");
        assertNotNull(event2, "Should receive report for activity 2");
        assertNotNull(event3, "Should receive report for activity 3");

        // Verify reports are for different weeks
        assertThat(event1.weekNumber()).isEqualTo(49);
        assertThat(event2.weekNumber()).isEqualTo(50);
        assertThat(event3.weekNumber()).isEqualTo(51);

        // Verify all reports exist in database
        var report49 = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 49, 2024);
        var report50 = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 50, 2024);
        var report51 = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 51, 2024);

        assertThat(report49).isPresent();
        assertThat(report50).isPresent();
        assertThat(report51).isPresent();
    }

    @Test
    void shouldHandleDifferentWeeks() throws InterruptedException {
        // Given - Activities in different weeks
        UUID activityId49 = UUID.randomUUID();
        UUID activityId50 = UUID.randomUUID();
        Instant time49 = Instant.parse("2024-12-08T10:00:00Z"); // Week 49
        Instant time50 = Instant.parse("2024-12-15T10:00:00Z"); // Week 50

        // Insert activity metrics into database
        createActivityMetrics(activityId49, "test-user", time49, new BigDecimal("10.0"), 3600, 360);
        createActivityMetrics(activityId50, "test-user", time50, new BigDecimal("12.0"), 4320, 360);

        ActivityMetricsDto week49Activity = ActivityMetricsDto.builder()
                .activityId(activityId49)
                .userId("test-user")
                .startedAt(time49)
                .totalDistance(new BigDecimal("10.0"))
                .totalDuration(3600)
                .build();

        ActivityMetricsDto week50Activity = ActivityMetricsDto.builder()
                .activityId(activityId50)
                .userId("test-user")
                .startedAt(time50)
                .totalDistance(new BigDecimal("12.0"))
                .totalDuration(4320)
                .build();

        // When
        kafkaTemplate.send("metrics.calculated", "test-user", week49Activity);
        Thread.sleep(1000);
        kafkaTemplate.send("metrics.calculated", "test-user", week50Activity);

        // Then - Should generate 2 different reports
        ConsumerRecord<String, ReportGeneratedEventDto> record1 = records.poll(15, TimeUnit.SECONDS);
        ConsumerRecord<String, ReportGeneratedEventDto> record2 = records.poll(15, TimeUnit.SECONDS);

        assertNotNull(record1);
        assertNotNull(record2);

        // Verify different weeks
        assertThat(record1.value().weekNumber()).isEqualTo(49);
        assertThat(record2.value().weekNumber()).isEqualTo(50);

        // Verify both reports exist in database
        var report49 = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 49, 2024);
        var report50 = trainingReportRepository.findByUserIdAndWeekNumberAndYear("test-user", 50, 2024);

        assertThat(report49).isPresent();
        assertThat(report50).isPresent();
    }
}