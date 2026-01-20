package com.runalitycs.report_generator.service;

import com.runalitycs.report_generator.entity.WeeklyStats;
import com.runalitycs.report_generator.entity.AthleteProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class MarkdownTemplateService {

    /**
     * Generate weekly training report in Markdown format
     */
    public String generateWeeklyReport(
            AthleteProfile profile,
            WeeklyStats currentWeek,
            List<WeeklyStats> last4Weeks
    ) {
        log.info("Generating markdown report for userId: {}, week: {}/{}",
                profile.getUserId(), currentWeek.getWeekNumber(), currentWeek.getYear());

        StringBuilder markdown = new StringBuilder();

        // Header
        markdown.append(String.format("# Training Report - Week %d/%d%n",
                currentWeek.getWeekNumber(), currentWeek.getYear()));
        markdown.append(String.format("**Athlete**: %s%n%n", profile.getName()));

        // This Week Summary
        appendWeeklySummary(markdown, currentWeek);

        // Last 4 Weeks Comparison
        append4WeeksComparison(markdown, last4Weeks);

        // Detailed Analysis
        appendDetailedAnalysis(markdown, currentWeek, profile);

        // HR Zones Distribution
        if (currentWeek.getHrZonesDistribution() != null && !currentWeek.getHrZonesDistribution().isEmpty()) {
            appendHrZonesDistribution(markdown, currentWeek);
        }
        // Goals
        appendGoals(markdown, profile);


        return markdown.toString();
    }

    private void appendWeeklySummary(StringBuilder markdown, WeeklyStats week) {
        markdown.append("## This Week Summary\n");
        markdown.append(String.format("- Total Activities: %d%n", week.getTotalActivities()));
        markdown.append(String.format(Locale.US, "- Total Distance: %.1f km%n", week.getTotalDistance()));
        markdown.append(String.format("- Total Duration: %s%n", formatDuration(week.getTotalDuration())));

        if (week.getAveragePace() != null) {
            markdown.append(String.format("- Average Pace: %s min/km%n", formatPace(week.getAveragePace())));
        }

        if (week.getAverageHeartRate() != null) {
            markdown.append(String.format("- Average Heart Rate: %d bpm%n", week.getAverageHeartRate()));
        }

        if (week.getAverageCadence() != null) {
            markdown.append(String.format("- Average Cadence: %d spm%n", week.getAverageCadence()));
        }

        markdown.append("\n");
    }

    private void append4WeeksComparison(StringBuilder markdown, List<WeeklyStats> weeks) {
        markdown.append("## Last 4 Weeks Comparison\n");
        markdown.append("| Week | Activities | Distance | Duration | Avg Pace |\n");
        markdown.append("|------|------------|----------|----------|----------|\n");

        for (WeeklyStats week : weeks) {
            markdown.append(String.format(Locale.US, "| %d/%d | %d | %.1f km | %s | %s |\n",
                    week.getWeekNumber(),
                    week.getYear(),
                    week.getTotalActivities(),
                    week.getTotalDistance(),
                    formatDuration(week.getTotalDuration()),
                    week.getAveragePace() != null ? formatPace(week.getAveragePace()) + " min/km" : "N/A"
            ));
        }

        markdown.append("\n");
    }

    private void appendDetailedAnalysis(StringBuilder markdown, WeeklyStats week, AthleteProfile profile) {
        markdown.append("## Detailed Analysis\n");

        // Volume analysis
        double distance = week.getTotalDistance().doubleValue();
        String volumeAnalysis;
        if (distance < 30) {
            volumeAnalysis = "Low volume (< 30 km). Consider gradually increasing weekly mileage.";
        } else if (distance < 60) {
            volumeAnalysis = "Moderate volume (30-60 km). Good base building.";
        } else {
            volumeAnalysis = "High volume (> 60 km). Excellent training load.";
        }
        markdown.append(String.format("**Volume**: %s%n", volumeAnalysis));

        // Pace analysis
        if (week.getAveragePace() != null) {
            String paceAnalysis;
            int pace = week.getAveragePace();
            if (pace < 240) {
                paceAnalysis = "Fast pace (< 4:00 min/km). High intensity training.";
            } else if (pace < 360) {
                paceAnalysis = "Moderate pace (4:00-6:00 min/km). Good aerobic zone.";
            } else {
                paceAnalysis = "Easy pace (> 6:00 min/km). Recovery/base building.";
            }
            markdown.append(String.format("**Pace**: %s%n", paceAnalysis));
        }

        // Cadence analysis
        if (week.getAverageCadence() != null) {
            String cadenceAnalysis;
            int cadence = week.getAverageCadence();
            if (cadence < 160) {
                cadenceAnalysis = String.format("Low cadence (%d spm). Target 170-180 spm to reduce injury risk.", cadence);
            } else if (cadence < 180) {
                cadenceAnalysis = String.format("Good cadence (%d spm). Within optimal range.", cadence);
            } else {
                cadenceAnalysis = String.format("High cadence (%d spm). Excellent running economy.", cadence);
            }
            markdown.append(String.format("**Cadence**: %s%n", cadenceAnalysis));
        }

        markdown.append("\n");
    }

    private void appendHrZonesDistribution(StringBuilder markdown, WeeklyStats week) {
        markdown.append("## Heart Rate Zones Distribution\n");
        markdown.append("| Zone | Time | Percentage |\n");
        markdown.append("|------|------|------------|\n");

        Map<String, Integer> hrZones = week.getHrZonesDistribution();
        int totalSeconds = hrZones.values().stream().mapToInt(Integer::intValue).sum();

        // Sort zones (Z1, Z2, Z3, Z4, Z5)
        hrZones.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String zone = entry.getKey();
                    int seconds = entry.getValue();
                    double percentage = (seconds * 100.0) / totalSeconds;

                    markdown.append(String.format(Locale.US, "| %s | %s | %.1f%% |\n",
                            zone,
                            formatDuration(seconds),
                            percentage
                    ));
                });

        markdown.append("\n");

        // Zone analysis
        appendHrZoneAnalysis(markdown, hrZones, totalSeconds);
    }

    private void appendHrZoneAnalysis(StringBuilder markdown, Map<String, Integer> hrZones, int totalSeconds) {
        markdown.append("**Zone Analysis**: ");

        // Calculate Z2 percentage (aerobic base)
        int z2Seconds = hrZones.getOrDefault("Z2", 0);
        double z2Percentage = (z2Seconds * 100.0) / totalSeconds;

        if (z2Percentage > 70) {
            markdown.append("Excellent Z2 focus (%.1f%%). Perfect for aerobic base building.\n");
        } else if (z2Percentage > 50) {
            markdown.append(String.format(Locale.US, "Good Z2 focus (%.1f%%). Solid aerobic training.\n", z2Percentage));
        } else {
            markdown.append(String.format(Locale.US, "Low Z2 time (%.1f%%). Consider more easy runs for base building.\n", z2Percentage));
        }

        markdown.append("\n");
    }

    private void appendGoals(StringBuilder markdown, AthleteProfile profile) {
        markdown.append("## Goals\n");

        if (profile.getCurrentGoal() != null && !profile.getCurrentGoal().isEmpty()) {
            markdown.append(String.format("**Current Goal**: %s%n", profile.getCurrentGoal()));
        } else {
            markdown.append("No current goal set.\n");
        }

        markdown.append("\n");
    }

    private String formatDuration(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%dh %02dm", hours, minutes);
    }

    private String formatPace(int secondsPerKm) {
        int minutes = secondsPerKm / 60;
        int seconds = secondsPerKm % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
