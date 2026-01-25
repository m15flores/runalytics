package com.runalytics.ai_coach.service;

import com.runalytics.ai_coach.dto.TrainingCycleContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service to determine training cycle context based on week number
 */
@Service
@Slf4j
public class TrainingCycleContextService {

    private static final int CYCLE_LENGTH = 4;

    /**
     * Determine training cycle context for a given week
     *
     * @param userId User ID (for future user-specific phase tracking)
     * @param weekNumber ISO week number (1-53)
     * @return Training cycle context
     */
    public TrainingCycleContext determineContext(String userId, Integer weekNumber) {

        // Calculate week in cycle (1-4)
        int weekInCycle = ((weekNumber - 1) % CYCLE_LENGTH) + 1;

        // Week 4 of each cycle is deload
        boolean isDeloadWeek = (weekInCycle == 4);

        // TODO: In future, this could be fetched from user preferences/training plan
        // For now, default to aerobic base phase
        TrainingCycleContext.TrainingPhase phase = TrainingCycleContext.TrainingPhase.AEROBIC_BASE;

        String primaryFocus = determinePrimaryFocus(weekInCycle, isDeloadWeek, phase);

        log.debug("Determined context for userId: {}, week: {} -> weekInCycle: {}, deload: {}, phase: {}",
                userId, weekNumber, weekInCycle, isDeloadWeek, phase);

        return TrainingCycleContext.builder()
                .weekInCycle(weekInCycle)
                .phase(phase)
                .primaryFocus(primaryFocus)
                .isDeloadWeek(isDeloadWeek)
                .targetZone("Zone 2")
                .build();
    }

    /**
     * Determine primary focus based on cycle context
     */
    private String determinePrimaryFocus(int weekInCycle, boolean isDeloadWeek,
                                         TrainingCycleContext.TrainingPhase phase) {
        if (isDeloadWeek) {
            return "Recovery and adaptation - reduced volume week";
        }

        if (phase == TrainingCycleContext.TrainingPhase.AEROBIC_BASE) {
            return switch (weekInCycle) {
                case 1 -> "Building aerobic base - focus on Z2 volume";
                case 2 -> "Increasing aerobic capacity - maintain Z2 compliance";
                case 3 -> "Peak aerobic volume week - prioritize recovery between sessions";
                default -> "Recovery week";
            };
        }

        if (phase == TrainingCycleContext.TrainingPhase.QUALITY_BLOCK) {
            return switch (weekInCycle) {
                case 1 -> "Introduction to quality work - maintain easy day discipline";
                case 2 -> "Building intensity tolerance - focus on workout execution";
                case 3 -> "Peak quality week - ensure adequate recovery";
                default -> "Recovery week";
            };
        }

        return "General training progression";
    }
}
