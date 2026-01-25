package com.runalytics.ai_coach.repository;

import com.runalytics.ai_coach.entity.Priority;
import com.runalytics.ai_coach.entity.Recommendation;
import com.runalytics.ai_coach.entity.RecommendationType;
import org.junit.jupiter.api.BeforeEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
public class RecommendationRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RecommendationRepository recommendationRepository;

    @BeforeEach
    void setUp() {
        recommendationRepository.deleteAll();
    }

    @Test
    void shouldSaveAndFindRecommendation() {
        // Given
        Recommendation recommendation = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.TRAINING_VOLUME)
                .priority(Priority.HIGH)
                .content("Reduce weekly mileage")
                .rationale("Volume spike detected")
                .build();

        // When
        Recommendation saved = recommendationRepository.save(recommendation);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        Optional<Recommendation> found = recommendationRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("test-user");
    }

    @Test
    void shouldFindRecommendationsByUserId() {
        // Given
        UUID reportId = UUID.randomUUID();

        Recommendation rec1 = Recommendation.builder()
                .userId("user-1")
                .reportId(reportId)
                .type(RecommendationType.TRAINING_VOLUME)
                .priority(Priority.HIGH)
                .content("Reduce volume")
                .rationale("Volume spike")
                .build();

        Recommendation rec2 = Recommendation.builder()
                .userId("user-1")
                .reportId(reportId)
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Take rest day")
                .rationale("Elevated heart rate")
                .build();

        Recommendation rec3 = Recommendation.builder()
                .userId("user-2")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.PACE)
                .priority(Priority.LOW)
                .content("Maintain pace")
                .rationale("Good consistency")
                .build();

        recommendationRepository.saveAll(List.of(rec1, rec2, rec3));

        // When
        List<Recommendation> user1Recommendations = recommendationRepository.findByUserId("user-1");

        // Then
        assertThat(user1Recommendations).hasSize(2);
        assertThat(user1Recommendations)
                .extracting(Recommendation::getUserId)
                .containsOnly("user-1");
    }

    @Test
    void shouldFindRecommendationsByReportId() {
        // Given
        UUID reportId1 = UUID.randomUUID();
        UUID reportId2 = UUID.randomUUID();

        Recommendation rec1 = Recommendation.builder()
                .userId("test-user")
                .reportId(reportId1)
                .type(RecommendationType.TRAINING_VOLUME)
                .priority(Priority.HIGH)
                .content("First recommendation")
                .rationale("First rationale")
                .build();

        Recommendation rec2 = Recommendation.builder()
                .userId("test-user")
                .reportId(reportId1)
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Second recommendation")
                .rationale("Second rationale")
                .build();

        Recommendation rec3 = Recommendation.builder()
                .userId("test-user")
                .reportId(reportId2)
                .type(RecommendationType.PACE)
                .priority(Priority.LOW)
                .content("Third recommendation")
                .rationale("Third rationale")
                .build();

        recommendationRepository.saveAll(List.of(rec1, rec2, rec3));

        // When
        List<Recommendation> report1Recommendations = recommendationRepository.findByReportId(reportId1);

        // Then
        assertThat(report1Recommendations).hasSize(2);
        assertThat(report1Recommendations)
                .extracting(Recommendation::getReportId)
                .containsOnly(reportId1);
    }

    @Test
    void shouldFindRecommendationsByUserIdAndPriority() {
        // Given
        Recommendation highPriority1 = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.INJURY_PREVENTION)
                .priority(Priority.HIGH)
                .content("Critical recommendation")
                .rationale("Injury risk detected")
                .build();

        Recommendation highPriority2 = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.TRAINING_VOLUME)
                .priority(Priority.HIGH)
                .content("Another critical recommendation")
                .rationale("Overtraining detected")
                .build();

        Recommendation mediumPriority = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Medium priority recommendation")
                .rationale("Could improve")
                .build();

        Recommendation differentUser = Recommendation.builder()
                .userId("other-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.PACE)
                .priority(Priority.HIGH)
                .content("Other user high priority")
                .rationale("Different user")
                .build();

        recommendationRepository.saveAll(List.of(highPriority1, highPriority2, mediumPriority, differentUser));

        // When
        List<Recommendation> highPriorityRecs = recommendationRepository
                .findByUserIdAndPriority("test-user", Priority.HIGH);

        // Then
        assertThat(highPriorityRecs).hasSize(2);
        assertThat(highPriorityRecs)
                .extracting(Recommendation::getPriority)
                .containsOnly(Priority.HIGH);
        assertThat(highPriorityRecs)
                .extracting(Recommendation::getUserId)
                .containsOnly("test-user");
    }

    @Test
    void shouldFindActiveRecommendationsByUserId() {
        // Given
        Instant now = Instant.now();
        Instant future = now.plusSeconds(7 * 24 * 3600); // 7 days from now
        Instant past = now.minusSeconds(3600); // 1 hour ago

        Recommendation active1 = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.TRAINING_VOLUME)
                .priority(Priority.HIGH)
                .content("Active recommendation 1")
                .rationale("Still valid")
                .expiresAt(future)
                .build();

        Recommendation active2 = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.RECOVERY)
                .priority(Priority.MEDIUM)
                .content("Active recommendation 2")
                .rationale("Still valid")
                .expiresAt(null) // No expiration
                .build();

        Recommendation expired = Recommendation.builder()
                .userId("test-user")
                .reportId(UUID.randomUUID())
                .type(RecommendationType.PACE)
                .priority(Priority.LOW)
                .content("Expired recommendation")
                .rationale("No longer valid")
                .expiresAt(past)
                .build();

        recommendationRepository.saveAll(List.of(active1, active2, expired));

        // When
        List<Recommendation> activeRecs = recommendationRepository
                .findActiveRecommendationsByUserId("test-user", now);

        // Then
        assertThat(activeRecs).hasSize(2);
        assertThat(activeRecs)
                .extracting(Recommendation::getContent)
                .containsExactlyInAnyOrder("Active recommendation 1", "Active recommendation 2");
    }
}
