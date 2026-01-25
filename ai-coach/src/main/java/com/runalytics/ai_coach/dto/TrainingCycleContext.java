package com.runalytics.ai_coach.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Training cycle context for AI analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingCycleContext {

    /**
     * Current week in the 4-week cycle (1-4)
     */
    private Integer weekInCycle;

    /**
     * Current training phase
     */
    private TrainingPhase phase;

    /**
     * Primary focus for current phase
     */
    private String primaryFocus;

    /**
     * Whether this is a recovery/deload week (typically week 4)
     */
    @Builder.Default
    private Boolean isDeloadWeek = false;

    /**
     * Target zone for aerobic base work (typically Z2)
     */
    @Builder.Default
    private String targetZone = "Zone 2";

    public enum TrainingPhase {
        AEROBIC_BASE,       // Z2-heavy phase
        QUALITY_BLOCK,      // Intervals, tempo work
        TRANSITION,         // Between phases
        TAPER,              // Pre-race reduction
        RECOVERY            // Post-race or deload
    }
}