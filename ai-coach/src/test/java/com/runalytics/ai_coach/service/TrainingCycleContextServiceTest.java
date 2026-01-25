package com.runalytics.ai_coach.service;

import com.runalytics.ai_coach.dto.TrainingCycleContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TrainingCycleContextServiceTest {

    private TrainingCycleContextService trainingCycleContextService;

    @BeforeEach
    void setUp() {
        trainingCycleContextService = new TrainingCycleContextService();
    }

    @Test
    void shouldDetermineWeekInCycleForWeek1() {
        // Given - Week 1 of year (ISO week)
        int weekNumber = 1;

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getWeekInCycle()).isEqualTo(1);
        assertThat(context.getIsDeloadWeek()).isFalse();
    }

    @Test
    void shouldDetermineWeekInCycleForWeek4() {
        // Given - Week 4 should be deload (4, 8, 12, 16...)
        int weekNumber = 4;

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getWeekInCycle()).isEqualTo(4);
        assertThat(context.getIsDeloadWeek()).isTrue();
    }

    @Test
    void shouldDetermineWeekInCycleForWeek5() {
        // Given - Week 5 = cycle 2, week 1
        int weekNumber = 5;

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getWeekInCycle()).isEqualTo(1);
        assertThat(context.getIsDeloadWeek()).isFalse();
    }

    @Test
    void shouldDetermineWeekInCycleForWeek8() {
        // Given - Week 8 = cycle 2, week 4 = deload
        int weekNumber = 8;

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getWeekInCycle()).isEqualTo(4);
        assertThat(context.getIsDeloadWeek()).isTrue();
    }

    @Test
    void shouldDefaultToAerobicBasePhase() {
        // Given
        int weekNumber = 10;

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getPhase()).isEqualTo(TrainingCycleContext.TrainingPhase.AEROBIC_BASE);
        assertThat(context.getPrimaryFocus()).contains("aerobic");
    }

    @Test
    void shouldSetCorrectPrimaryFocusForDeloadWeek() {
        // Given
        int weekNumber = 12; // Week 4 of cycle 3

        // When
        TrainingCycleContext context = trainingCycleContextService
                .determineContext("test-user", weekNumber);

        // Then
        assertThat(context.getIsDeloadWeek()).isTrue();
        assertThat(context.getPrimaryFocus()).containsIgnoringCase("recovery");
    }
}
