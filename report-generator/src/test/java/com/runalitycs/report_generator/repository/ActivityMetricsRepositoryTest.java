package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.ActivityMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
public class ActivityMetricsRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ActivityMetricsRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        jdbcTemplate.execute("DELETE FROM activity_metrics");
    }

    @Test
    void shouldReadActivityMetricsFromDatabase() {
        // Given - Insert data directly via SQL (simulating metrics-engine)
        UUID activityId = UUID.randomUUID();
        UUID metricsId = UUID.randomUUID();
        Instant now = Instant.now();

        String sql = """
                INSERT INTO activity_metrics (
                    id, activity_id, user_id, started_at, total_distance, 
                    total_duration, total_calories, average_pace, 
                    average_heart_rate, average_cadence, total_ascent, 
                    total_descent, training_effect, anaerobic_training_effect, 
                    calculated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                metricsId, activityId, "test-user", Timestamp.from(now),
                new BigDecimal("10.5"), 3600, 600, 360,
                145, 170, 50, 50, 3.5, 1.2, Timestamp.from(now)
        );

        // When
        ActivityMetrics metrics = repository.findById(metricsId).orElse(null);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics.getId()).isEqualTo(metricsId);
        assertThat(metrics.getActivityId()).isEqualTo(activityId);
        assertThat(metrics.getUserId()).isEqualTo("test-user");
        assertThat(metrics.getTotalDistance()).isEqualByComparingTo(new BigDecimal("10.5"));
        assertThat(metrics.getTotalDuration()).isEqualTo(3600);
        assertThat(metrics.getAveragePace()).isEqualTo(360);
        assertThat(metrics.getAverageHeartRate()).isEqualTo(145);
    }

    @Test
    void shouldFindActivitiesByUserIdAndDateRange() {
        // Given - Insert 3 activities for user, 1 for another user
        Instant now = Instant.now();
        Instant yesterday = now.minusSeconds(86400);
        Instant twoDaysAgo = now.minusSeconds(172800);
        Instant threeDaysAgo = now.minusSeconds(259200);

        insertActivityMetric("test-user", now);
        insertActivityMetric("test-user", yesterday);
        insertActivityMetric("test-user", threeDaysAgo); // Outside range
        insertActivityMetric("other-user", yesterday);

        // When
        List<ActivityMetrics> metrics = repository.findByUserIdAndDateRange(
                "test-user",
                twoDaysAgo,
                now.plusSeconds(1)
        );

        // Then
        assertEquals(2, metrics.size());
        assertThat(metrics).allMatch(m -> m.getUserId().equals("test-user"));
        assertThat(metrics.get(0).getStartedAt()).isAfter(metrics.get(1).getStartedAt()); // Ordered DESC
    }

    @Test
    void shouldReturnEmptyWhenNoActivitiesFound() {
        // Given - No activities in database
        Instant now = Instant.now();
        Instant yesterday = now.minusSeconds(86400);

        // When
        List<ActivityMetrics> metrics = repository.findByUserIdAndDateRange(
                "non-existent-user",
                yesterday,
                now
        );

        // Then
        assertEquals(0, metrics.size());
    }

    // Helper method
    private void insertActivityMetric(String userId, Instant startedAt) {
        String sql = """
                INSERT INTO activity_metrics (
                    id, activity_id, user_id, started_at, total_distance, 
                    total_duration, average_pace, calculated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(sql,
                UUID.randomUUID(),
                UUID.randomUUID(),
                userId,
                Timestamp.from(startedAt),
                new BigDecimal("10.0"),
                3600,
                360,
                Timestamp.from(Instant.now())
        );
    }
}
