package com.runalytics.metrics_engine.repository;

import com.runalytics.metrics_engine.entity.ActivityMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ActivityMetricsRepositoryTest {

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

    @Test
    void shouldSaveAndFindByActivityId() {
        // Given
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.parse("2025-01-01T10:00:00Z");

        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setActivityId(activityId);
        metrics.setUserId("test-user");
        metrics.setTotalDistance(new BigDecimal("10000"));
        metrics.setTotalDuration(3600);
        metrics.setCalculatedAt(now);
        metrics.setCreatedAt(now);
        metrics.setUpdatedAt(now);

        // When
        repository.save(metrics);
        Optional<ActivityMetrics> found = repository.findByActivityId(activityId);

        // Then
        assertTrue(found.isPresent());
        assertEquals(activityId, found.get().getActivityId());
        assertEquals("test-user", found.get().getUserId());
    }

    @Test
    void shouldCheckExistsByActivityId() {
        // Given
        UUID activityId = UUID.randomUUID();
        Instant now = Instant.parse("2025-01-01T10:00:00Z");

        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setActivityId(activityId);
        metrics.setUserId("test-user");
        metrics.setTotalDistance(new BigDecimal("10000"));
        metrics.setTotalDuration(3600);
        metrics.setCalculatedAt(now);
        metrics.setCreatedAt(now);
        metrics.setUpdatedAt(now);

        repository.save(metrics);

        // When
        boolean exists = repository.existsByActivityId(activityId);
        boolean notExists = repository.existsByActivityId(UUID.randomUUID());

        // Then
        assertTrue(exists);
        assertFalse(notExists);
    }
}
