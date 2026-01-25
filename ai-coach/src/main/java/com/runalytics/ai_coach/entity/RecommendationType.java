package com.runalytics.ai_coach.entity;

public enum RecommendationType {
    // Safety (highest priority)
    INJURY_PREVENTION,      // Injury risk detected
    OVERTRAINING,           // Overtraining signals

    // Zone compliance (core of training)
    ZONE_COMPLIANCE,        // Z2 or intended zone not respected
    CARDIAC_DRIFT,          // HR drift indicating aerobic inefficiency

    // Training quality
    TRAINING_VOLUME,        // Volume adjustments
    RECOVERY,               // Recovery adequacy
    WORKOUT_QUALITY,        // Training effectiveness assessment

    // Secondary optimizations
    PACE,                   // Pace adjustments (consequence, not goal)
    CADENCE,                // Cadence optimization
    HEART_RATE,             // General HR observations

    // Progress tracking
    GOAL_PROGRESS,          // Progress towards stated goal
    NUTRITION               // Nutrition-related
}