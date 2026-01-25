package com.runalytics.ai_coach.entity;

/**
 * Overall assessment of training quality/validity
 */
public enum TrainingVerdict {
    VALID,              // Training fulfilled its purpose
    PARTIALLY_VALID,    // Some concerns but acceptable
    INVALID             // Did not fulfill intended purpose
}