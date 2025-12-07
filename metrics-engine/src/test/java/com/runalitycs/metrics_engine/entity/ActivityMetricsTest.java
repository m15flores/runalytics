package com.runalitycs.metrics_engine.entity;

import com.runalitycs.metrics_engine.entity.ActivityMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
class ActivityMetricsTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistActivityMetricsWithJsonbFields() {
        // Given
        ActivityMetrics metrics = new ActivityMetrics();
        metrics.setActivityId(UUID.randomUUID());
        metrics.setUserId("test-user");
        metrics.setTotalDistance(new BigDecimal("13138.37"));
        metrics.setTotalDuration(4777);
        metrics.setTotalCalories(1048);

        // HR Zones como JSONB
        Map<String, Integer> hrZones = Map.of(
                "Z1", 0,
                "Z2", 2580,
                "Z3", 2010,
                "Z4", 187,
                "Z5", 0
        );
        metrics.setHrZones(hrZones);

        Map<String, Integer> hrZonesPercentage = Map.of(
                "Z1", 0,
                "Z2", 54,
                "Z3", 42,
                "Z4", 4,
                "Z5", 0
        );
        metrics.setHrZonesPercentage(hrZonesPercentage);

        // When
        ActivityMetrics saved = entityManager.persistAndFlush(metrics);
        entityManager.clear();  // Limpiar cache para forzar lectura de BD

        ActivityMetrics loaded = entityManager.find(ActivityMetrics.class, saved.getId());

        // Then
        assertNotNull(loaded);
        assertNotNull(loaded.getId());
        assertEquals("test-user", loaded.getUserId());
        assertEquals(new BigDecimal("13138.37"), loaded.getTotalDistance());

        // Verificar timestamps
        assertNotNull(loaded.getCreatedAt());
        assertNotNull(loaded.getUpdatedAt());
        assertNotNull(loaded.getCalculatedAt());

        // Verificar JSONB
        assertNotNull(loaded.getHrZones());
        assertEquals(5, loaded.getHrZones().size());
        assertEquals(2580, loaded.getHrZones().get("Z2"));

        assertNotNull(loaded.getHrZonesPercentage());
        assertEquals(54, loaded.getHrZonesPercentage().get("Z2"));
    }
}