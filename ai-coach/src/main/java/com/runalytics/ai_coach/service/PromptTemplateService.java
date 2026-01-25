package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.dto.TrainingCycleContext;
import com.runalytics.ai_coach.dto.TrainingReportDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTemplateService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Build system prompt with coaching philosophy
     */
    public String buildSystemPrompt(TrainingCycleContext cycleContext) {
        String phaseGuidance = getPhaseSpecificGuidance(cycleContext);

        return String.format(Locale.US, """
                You are an expert running coach with deep knowledge of polarized training, \
                aerobic development, and evidence-based endurance training principles.
                
                COACHING PHILOSOPHY (NON-NEGOTIABLE):
                %s
                
                PHYSIOLOGICAL HIERARCHY (in order of importance):
                1. Zone compliance (especially Zone 2 during aerobic base)
                2. Cardiac drift (primary signal of aerobic efficiency)
                3. Capacity to sustain duration without HR escalation
                4. Recovery adequacy between sessions
                5. Pace (a CONSEQUENCE of fitness, NOT a primary goal)
                
                EVALUATION FRAMEWORK:
                - You are authorized to INVALIDATE workouts that don't fulfill their purpose
                - "Zone 2" that isn't actually Z2 (HR drift >5%%, sustained Z3) = INVALID
                - Training that doesn't respect intended intensity = INVALID or PARTIALLY_VALID
                - Be direct and corrective, not diplomatic
                - Call out ego-driven training (going too fast when slow was prescribed)
                - Prioritize long-term process over short-term satisfaction
                
                OUTPUT FORMAT (strict JSON):
                {
                  "verdict": "VALID | PARTIALLY_VALID | INVALID",
                  "verdict_rationale": "Brief explanation of overall assessment",
                  "recommendations": [
                    {
                      "type": "ZONE_COMPLIANCE | CARDIAC_DRIFT | INJURY_PREVENTION | OVERTRAINING | TRAINING_VOLUME | RECOVERY | WORKOUT_QUALITY | PACE | CADENCE | HEART_RATE | GOAL_PROGRESS | NUTRITION",
                      "priority": "HIGH | MEDIUM | LOW",
                      "category": "RISK | ADJUSTMENT | CONFIRMATION",
                      "content": "Direct, specific, actionable recommendation (2-3 sentences max)",
                      "rationale": "Why this matters physiologically (1-2 sentences)"
                    }
                  ]
                }
                
                PRIORITY GUIDELINES:
                - HIGH: Safety issues, zone violations during base phase, severe overtraining signals
                - MEDIUM: Suboptimal training execution, recovery concerns, volume adjustments
                - LOW: Fine-tuning, confirmations of good execution
                
                CATEGORY GUIDELINES:
                - RISK: Safety concerns, injury prevention, overtraining
                - ADJUSTMENT: Corrective actions needed for training quality
                - CONFIRMATION: Validation of correct execution
                
                RULES:
                - Generate 3-5 recommendations maximum
                - Be specific with numbers and percentages
                - Base everything on actual data, not generic advice
                - Don't soften criticism - direct feedback drives adaptation
                - Consider the 10%% weekly volume increase rule
                - Cardiac drift >5%% in steady Z2 = red flag
                - Going "a bit fast" in Z2 work = training failure, not minor issue
                """,
                phaseGuidance
        );
    }

    /**
     * Get phase-specific coaching guidance
     */
    private String getPhaseSpecificGuidance(TrainingCycleContext context) {
        if (context.getPhase() == TrainingCycleContext.TrainingPhase.AEROBIC_BASE) {
            return String.format(Locale.US, """
                    CURRENT PHASE: Aerobic Base Development (Week %d of 4-week cycle)
                    %s
                    PRIMARY FOCUS: %s
                    
                    KEY RULES FOR THIS PHASE:
                    - Zone 2 compliance is THE priority - no exceptions
                    - Pace is irrelevant - only HR and perceived effort matter
                    - Cardiac drift in Z2 work is a primary indicator of aerobic fitness
                    - Volume increases must be gradual and sustainable
                    - ANY Z2 work that drifts into Z3 = failed workout
                    - "Feeling good" is NOT permission to go faster
                    - Base phase is about volume in correct zones, not performance
                    """,
                    context.getWeekInCycle(),
                    context.getIsDeloadWeek() ? "⚠️ DELOAD WEEK - Reduced volume expected" : "",
                    context.getPrimaryFocus() != null ? context.getPrimaryFocus() : "Building aerobic base"
            );
        }

        if (context.getPhase() == TrainingCycleContext.TrainingPhase.QUALITY_BLOCK) {
            return String.format(Locale.US, """
                    CURRENT PHASE: Quality/Intensity Block (Week %d of 4-week cycle)
                    %s
                    PRIMARY FOCUS: %s
                    
                    KEY RULES FOR THIS PHASE:
                    - Quality sessions must hit target zones precisely
                    - Recovery between quality sessions is critical
                    - Easy days must stay EASY (genuine Z1-Z2)
                    - Volume is secondary to intensity execution
                    - Cardiac drift in recovery runs still matters
                    """,
                    context.getWeekInCycle(),
                    context.getIsDeloadWeek() ? "⚠️ DELOAD WEEK - Reduced intensity expected" : "",
                    context.getPrimaryFocus() != null ? context.getPrimaryFocus() : "Building quality"
            );
        }

        // Default for other phases
        return String.format(Locale.US, """
                CURRENT PHASE: %s (Week %d of 4-week cycle)
                %s
                PRIMARY FOCUS: %s
                """,
                context.getPhase(),
                context.getWeekInCycle(),
                context.getIsDeloadWeek() ? "⚠️ DELOAD WEEK" : "",
                context.getPrimaryFocus() != null ? context.getPrimaryFocus() : "General training"
        );
    }

    /**
     * Build user prompt with training report data and cycle context
     */
    public String buildUserPrompt(TrainingReportDto report, TrainingCycleContext cycleContext) {
        try {
            JsonNode summaryJson = objectMapper.readTree(report.getSummaryJson());

            String athleteName = extractAthleteName(report.getMarkdownContent());
            String currentGoal = extractGoal(report.getMarkdownContent());

            return String.format(Locale.US, """
                    Analyze this training report for %s:
                    
                    TRAINING CONTEXT:
                    - Week %d/%d (Week %d of current 4-week cycle)
                    - Phase: %s
                    - Deload week: %s
                    - Primary focus: %s
                    
                    WEEK SUMMARY:
                    - Total Distance: %.1f km
                    - Total Duration: %s
                    - Average Pace: %s
                    - Average Heart Rate: %d bpm
                    - Total Activities: %d
                    
                    4-WEEK TREND:
                    %s
                    
                    HEART RATE ZONES DISTRIBUTION:
                    %s
                    
                    ATHLETE'S STATED GOAL:
                    %s
                    
                    CRITICAL EVALUATION REQUIRED:
                    1. Zone Compliance: Did the athlete respect intended zones (especially Z2)?
                    2. Cardiac Drift: Any concerning HR drift in aerobic work?
                    3. Training Purpose: Did this week fulfill its intended function?
                    4. Recovery Signals: Are there signs of inadequate recovery?
                    5. Volume Progression: Is the week-to-week increase sustainable?
                    
                    Provide:
                    - Overall verdict (VALID/PARTIALLY_VALID/INVALID)
                    - 3-5 specific recommendations categorized by RISK/ADJUSTMENT/CONFIRMATION
                    - Be direct about failures - sugar-coating helps nobody
                    - If Z2 work drifted into Z3, call it out explicitly
                    - If volume jumped unsafely, flag it
                    """,
                    athleteName,
                    report.getWeekNumber(),
                    report.getYear(),
                    cycleContext.getWeekInCycle(),
                    cycleContext.getPhase(),
                    cycleContext.getIsDeloadWeek() ? "YES" : "NO",
                    cycleContext.getPrimaryFocus(),
                    summaryJson.has("totalKm") ? summaryJson.get("totalKm").asDouble() : 0.0,
                    formatDuration(summaryJson.has("totalDuration") ? summaryJson.get("totalDuration").asInt() : 0),
                    formatPace(summaryJson.has("averagePace") ? summaryJson.get("averagePace").asInt() : 0),
                    summaryJson.has("averageHeartRate") ? summaryJson.get("averageHeartRate").asInt() : 0,
                    summaryJson.has("totalActivities") ? summaryJson.get("totalActivities").asInt() : 0,
                    extractComparisonTable(report.getMarkdownContent()),
                    extractHrZones(report.getMarkdownContent()),
                    currentGoal
            );

        } catch (Exception e) {
            log.error("Error building user prompt: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to build user prompt", e);
        }
    }

    // Keep existing helper methods: extractAthleteName, extractGoal, extractComparisonTable,
    // extractHrZones, formatDuration, formatPace

    private String extractAthleteName(String markdown) {
        String[] lines = markdown.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Training Report - Week")) {
                if (i + 1 < lines.length && lines[i + 1].startsWith("Athlete:")) {
                    return lines[i + 1].replace("Athlete:", "").trim();
                }
            }
        }
        return "Athlete";
    }

    private String extractGoal(String markdown) {
        String[] lines = markdown.split("\n");
        for (String line : lines) {
            if (line.contains("Goal:") || line.contains("Current Goal:")) {
                return line.replaceAll(".*Goal:", "").trim();
            }
        }
        return "Not specified";
    }

    private String extractComparisonTable(String markdown) {
        StringBuilder table = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inTable = false;

        for (String line : lines) {
            if (line.contains("4-Week Comparison") || line.contains("Last 4 Weeks")) {
                inTable = true;
                continue;
            }
            if (inTable && line.trim().isEmpty()) {
                break;
            }
            if (inTable && (line.startsWith("|") || line.contains("Week"))) {
                table.append(line).append("\n");
            }
        }

        return table.length() > 0 ? table.toString() : "No comparison data available";
    }

    private String extractHrZones(String markdown) {
        StringBuilder zones = new StringBuilder();
        String[] lines = markdown.split("\n");
        boolean inZones = false;

        for (String line : lines) {
            if (line.contains("Heart Rate Zones") || line.contains("HR Zones")) {
                inZones = true;
                continue;
            }
            if (inZones && line.trim().isEmpty()) {
                break;
            }
            if (inZones && (line.startsWith("|") || line.contains("Zone"))) {
                zones.append(line).append("\n");
            }
        }

        return zones.length() > 0 ? zones.toString() : "No HR zone data available";
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format(Locale.US, "%dh %02dm", hours, minutes);
    }

    private String formatPace(int secondsPerKm) {
        if (secondsPerKm <= 0) {
            return "N/A";
        }
        int minutes = secondsPerKm / 60;
        int seconds = secondsPerKm % 60;
        return String.format(Locale.US, "%d:%02d min/km", minutes, seconds);
    }
}