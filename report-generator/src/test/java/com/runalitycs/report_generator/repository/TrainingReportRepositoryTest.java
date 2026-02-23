package com.runalitycs.report_generator.repository;

import com.runalitycs.report_generator.entity.TrainingReport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Testcontainers
public class TrainingReportRepositoryTest {

    private static final Instant NOW = Instant.parse("2024-12-10T12:00:00Z");

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
    private TrainingReportRepository repository;

    @Test
    void shouldSaveTrainingReport() {
        // Given
        TrainingReport report = TrainingReport.builder()
                .userId("test-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Training Report Week 49")
                .summaryJson("{\"totalKm\": 52.0}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        // When
        TrainingReport saved = repository.save(report);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo("test-user");
        assertThat(saved.getWeekNumber()).isEqualTo(49);
        assertThat(saved.getYear()).isEqualTo(2024);
        assertThat(saved.getMarkdownContent()).isEqualTo("# Training Report Week 49");
        assertThat(saved.getSummaryJson()).isEqualTo("{\"totalKm\": 52.0}");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTriggerActivityId()).isNotNull();
    }

    @Test
    void shouldFindReportByUserIdAndWeekAndYear() {
        // Given
        TrainingReport report = TrainingReport.builder()
                .userId("test-runner")
                .weekNumber(48)
                .year(2024)
                .markdownContent("# Week 48 Report")
                .summaryJson("{\"totalKm\": 45.0}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        repository.save(report);

        // When
        Optional<TrainingReport> found = repository.findByUserIdAndWeekNumberAndYear(
                "test-runner", 48, 2024
        );

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("test-runner");
        assertThat(found.get().getWeekNumber()).isEqualTo(48);
        assertThat(found.get().getYear()).isEqualTo(2024);
    }

    @Test
    void shouldNotAllowDuplicateWeekForSameUser() {
        // Given
        TrainingReport report1 = TrainingReport.builder()
                .userId("duplicate-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# First Report")
                .summaryJson("{\"totalKm\": 50.0}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        TrainingReport report2 = TrainingReport.builder()
                .userId("duplicate-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Second Report")
                .summaryJson("{\"totalKm\": 55.0}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        repository.save(report1);

        // When & Then
        assertThatThrownBy(() -> {
            repository.save(report2);
            repository.flush(); // Force database write
        }).isInstanceOf(Exception.class); // Could be DataIntegrityViolationException
    }

    @Test
    void shouldFindAllReportsByUserOrderedByDate() {
        // Given
        TrainingReport week47 = TrainingReport.builder()
                .userId("ordered-user")
                .weekNumber(47)
                .year(2024)
                .markdownContent("# Week 47")
                .summaryJson("{}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        TrainingReport week49 = TrainingReport.builder()
                .userId("ordered-user")
                .weekNumber(49)
                .year(2024)
                .markdownContent("# Week 49")
                .summaryJson("{}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        TrainingReport week48 = TrainingReport.builder()
                .userId("ordered-user")
                .weekNumber(48)
                .year(2024)
                .markdownContent("# Week 48")
                .summaryJson("{}")
                .triggerActivityId(UUID.randomUUID())
                .createdAt(NOW)
                .build();

        // Save in random order
        repository.save(week47);
        repository.save(week49);
        repository.save(week48);

        // When
        List<TrainingReport> reports = repository.findByUserIdOrderByYearDescWeekNumberDesc("ordered-user");

        // Then
        assertEquals(3, reports.size());
        assertThat(reports.get(0).getWeekNumber()).isEqualTo(49); // Most recent first
        assertThat(reports.get(1).getWeekNumber()).isEqualTo(48);
        assertThat(reports.get(2).getWeekNumber()).isEqualTo(47);
    }
}
