package com.runalytics.normalizer.service;

import com.garmin.fit.*;
import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class FitParserService {

    public ParsedFitData parse(InputStream fitFileStream) {
        if (fitFileStream == null) {
            throw new IllegalArgumentException("FIT file stream cannot be null");
        }

        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);

        FitDataCollector collector = new FitDataCollector();

        mesgBroadcaster.addListener(new SessionMesgListener() {
            @Override
            public void onMesg(SessionMesg mesg) {
                collector.onSession(mesg);
            }
        });

        mesgBroadcaster.addListener(new LapMesgListener() {
            @Override
            public void onMesg(LapMesg mesg) {
                collector.onLap(mesg);
            }
        });

        mesgBroadcaster.addListener(new RecordMesgListener() {
            @Override
            public void onMesg(RecordMesg mesg) {
                collector.onRecord(mesg);
            }
        });

        try {
            if (!decode.read(fitFileStream, mesgBroadcaster)) {
                throw new IllegalArgumentException("Failed to decode FIT file");
            }
        } catch (FitRuntimeException e) {
            throw new IllegalArgumentException("Error parsing FIT file", e);
        }

        return collector.build();
    }

    /**
     * Collects data from FIT messages during parsing.
     */
    private static class FitDataCollector {
        private Instant startedAt;
        private Integer durationSeconds;
        private BigDecimal distanceMeters;
        private ParsedFitData.SessionInfo sessionInfo;
        private final List<ParsedFitData.LapInfo> laps = new ArrayList<>();
        private final List<ActivitySample> samples = new ArrayList<>();
        private int lapCounter = 0;

        public void onSession(SessionMesg mesg) {
            if (mesg.getStartTime() != null) {
                startedAt = convertFitTimestamp(mesg.getStartTime());
            }
            if (mesg.getTotalElapsedTime() != null) {
                durationSeconds = mesg.getTotalElapsedTime().intValue();
            }
            if (mesg.getTotalDistance() != null) {
                distanceMeters = BigDecimal.valueOf(mesg.getTotalDistance());
            }
            sessionInfo = buildSessionInfo(mesg);
        }

        private ParsedFitData.SessionInfo buildSessionInfo(SessionMesg mesg) {
            BigDecimal totalDistance = mesg.getTotalDistance() != null
                    ? BigDecimal.valueOf(mesg.getTotalDistance()) : null;
            Integer totalTimerTime = mesg.getTotalTimerTime() != null
                    ? mesg.getTotalTimerTime().intValue() : null;
            Integer totalElapsedTime = mesg.getTotalElapsedTime() != null
                    ? mesg.getTotalElapsedTime().intValue() : null;
            Integer totalCalories = mesg.getTotalCalories() != null
                    ? mesg.getTotalCalories() : null;

            Integer avgHR = mesg.getAvgHeartRate() != null
                    ? mesg.getAvgHeartRate().intValue() : null;
            Integer maxHR = mesg.getMaxHeartRate() != null
                    ? mesg.getMaxHeartRate().intValue() : null;
            // FIT cadence = strides/min (one foot) — multiply by 2 for total steps/min (SPM)
            Integer avgCadence = mesg.getAvgCadence() != null
                    ? mesg.getAvgCadence().intValue() * 2 : null;
            Integer maxCadence = mesg.getMaxCadence() != null
                    ? mesg.getMaxCadence().intValue() * 2 : null;

            BigDecimal enhancedAvgSpeed = mesg.getEnhancedAvgSpeed() != null
                    ? BigDecimal.valueOf(mesg.getEnhancedAvgSpeed()) : null;
            BigDecimal enhancedMaxSpeed = mesg.getEnhancedMaxSpeed() != null
                    ? BigDecimal.valueOf(mesg.getEnhancedMaxSpeed()) : null;

            Integer avgPower = mesg.getAvgPower() != null
                    ? mesg.getAvgPower() : null;
            Integer maxPower = mesg.getMaxPower() != null
                    ? mesg.getMaxPower() : null;
            Integer normalizedPower = mesg.getNormalizedPower() != null
                    ? mesg.getNormalizedPower() : null;

            Double avgVerticalOscillation = mesg.getAvgVerticalOscillation() != null
                    ? mesg.getAvgVerticalOscillation().doubleValue() : null;
            Double avgStanceTime = mesg.getAvgStanceTime() != null
                    ? mesg.getAvgStanceTime().doubleValue() : null;
            Double avgVerticalRatio = mesg.getAvgVerticalRatio() != null
                    ? mesg.getAvgVerticalRatio().doubleValue() : null;
            Integer avgStepLength = mesg.getAvgStepLength() != null
                    ? mesg.getAvgStepLength().intValue() : null;

            Integer totalAscent = mesg.getTotalAscent() != null
                    ? mesg.getTotalAscent() : null;
            Integer totalDescent = mesg.getTotalDescent() != null
                    ? mesg.getTotalDescent() : null;

            Double totalTrainingEffect = mesg.getTotalTrainingEffect() != null
                    ? mesg.getTotalTrainingEffect().doubleValue() : null;
            Double totalAnaerobicTrainingEffect = mesg.getTotalAnaerobicTrainingEffect() != null
                    ? mesg.getTotalAnaerobicTrainingEffect().doubleValue() : null;
            Double trainingLoadPeak = mesg.getTrainingLoadPeak() != null
                    ? mesg.getTrainingLoadPeak().doubleValue() : null;

            Integer workoutFeel = mesg.getWorkoutFeel() != null
                    ? mesg.getWorkoutFeel().intValue() : null;
            Integer workoutRpe = mesg.getWorkoutRpe() != null
                    ? mesg.getWorkoutRpe().intValue() : null;

            Map<String, Integer> timeInHrZones = extractHrZones(mesg);
            Map<String, Integer> timeInPowerZones = extractPowerZones(mesg);

            return new ParsedFitData.SessionInfo(
                    totalDistance, totalTimerTime, totalElapsedTime, totalCalories,
                    avgHR, maxHR, avgCadence, maxCadence,
                    enhancedAvgSpeed, enhancedMaxSpeed,
                    avgPower, maxPower, normalizedPower,
                    avgVerticalOscillation, avgStanceTime, avgVerticalRatio, avgStepLength,
                    totalAscent, totalDescent,
                    totalTrainingEffect, totalAnaerobicTrainingEffect, trainingLoadPeak,
                    workoutFeel, workoutRpe,
                    timeInHrZones, timeInPowerZones,
                    null, null, null, null // config fields come from UserProfile/ZonesTarget messages
            );
        }

        private Map<String, Integer> extractHrZones(SessionMesg mesg) {
            String[] zoneNames = {"Z1", "Z2", "Z3", "Z4", "Z5"};
            Map<String, Integer> zones = new LinkedHashMap<>();
            for (int i = 0; i < zoneNames.length; i++) {
                Float zoneTime = mesg.getTimeInHrZone(i);
                if (zoneTime != null) {
                    zones.put(zoneNames[i], zoneTime.intValue());
                }
            }
            return zones.isEmpty() ? null : zones;
        }

        private Map<String, Integer> extractPowerZones(SessionMesg mesg) {
            String[] zoneNames = {"Z1", "Z2", "Z3", "Z4", "Z5", "Z6", "Z7"};
            Map<String, Integer> zones = new LinkedHashMap<>();
            for (int i = 0; i < zoneNames.length; i++) {
                Float zoneTime = mesg.getTimeInPowerZone(i);
                if (zoneTime != null) {
                    zones.put(zoneNames[i], zoneTime.intValue());
                }
            }
            return zones.isEmpty() ? null : zones;
        }

        public void onLap(LapMesg mesg) {
            lapCounter++;

            Instant startTime = mesg.getStartTime() != null
                    ? convertFitTimestamp(mesg.getStartTime()) : null;
            BigDecimal totalDistance = mesg.getTotalDistance() != null
                    ? BigDecimal.valueOf(mesg.getTotalDistance()) : null;
            Integer totalTimerTime = mesg.getTotalTimerTime() != null
                    ? mesg.getTotalTimerTime().intValue() : null;
            Integer totalElapsedTime = mesg.getTotalElapsedTime() != null
                    ? mesg.getTotalElapsedTime().intValue() : null;
            Integer totalCalories = mesg.getTotalCalories() != null
                    ? mesg.getTotalCalories() : null;

            Integer avgHR = mesg.getAvgHeartRate() != null
                    ? mesg.getAvgHeartRate().intValue() : null;
            Integer maxHR = mesg.getMaxHeartRate() != null
                    ? mesg.getMaxHeartRate().intValue() : null;
            // FIT cadence = strides/min (one foot) — multiply by 2 for total steps/min (SPM)
            Integer avgCadence = mesg.getAvgCadence() != null
                    ? mesg.getAvgCadence().intValue() * 2 : null;
            Integer maxCadence = mesg.getMaxCadence() != null
                    ? mesg.getMaxCadence().intValue() * 2 : null;

            BigDecimal enhancedAvgSpeed = mesg.getEnhancedAvgSpeed() != null
                    ? BigDecimal.valueOf(mesg.getEnhancedAvgSpeed()) : null;
            BigDecimal enhancedMaxSpeed = mesg.getEnhancedMaxSpeed() != null
                    ? BigDecimal.valueOf(mesg.getEnhancedMaxSpeed()) : null;

            Integer avgPower = mesg.getAvgPower() != null
                    ? mesg.getAvgPower() : null;
            Integer maxPower = mesg.getMaxPower() != null
                    ? mesg.getMaxPower() : null;
            Integer normalizedPower = mesg.getNormalizedPower() != null
                    ? mesg.getNormalizedPower() : null;

            Double avgVerticalOscillation = mesg.getAvgVerticalOscillation() != null
                    ? mesg.getAvgVerticalOscillation().doubleValue() : null;
            Double avgStanceTime = mesg.getAvgStanceTime() != null
                    ? mesg.getAvgStanceTime().doubleValue() : null;
            Double avgVerticalRatio = mesg.getAvgVerticalRatio() != null
                    ? mesg.getAvgVerticalRatio().doubleValue() : null;
            Integer avgStepLength = mesg.getAvgStepLength() != null
                    ? mesg.getAvgStepLength().intValue() : null;

            Integer totalAscent = mesg.getTotalAscent() != null
                    ? mesg.getTotalAscent() : null;
            Integer totalDescent = mesg.getTotalDescent() != null
                    ? mesg.getTotalDescent() : null;

            String intensity = mesg.getIntensity() != null
                    ? mesg.getIntensity().name().toLowerCase() : null;
            Integer wktStepIndex = mesg.getWktStepIndex() != null
                    ? mesg.getWktStepIndex() : null;

            laps.add(new ParsedFitData.LapInfo(
                    lapCounter, startTime,
                    totalDistance, totalTimerTime, totalElapsedTime, totalCalories,
                    avgHR, maxHR, avgCadence, maxCadence,
                    enhancedAvgSpeed, enhancedMaxSpeed,
                    avgPower, maxPower, normalizedPower,
                    avgVerticalOscillation, avgStanceTime, avgVerticalRatio, avgStepLength,
                    totalAscent, totalDescent,
                    intensity, wktStepIndex
            ));
        }

        public void onRecord(RecordMesg mesg) {
            Instant timestamp = mesg.getTimestamp() != null
                    ? convertFitTimestamp(mesg.getTimestamp())
                    : null;

            if (timestamp == null) {
                return; // skip records without timestamp
            }

            Double latitude = mesg.getPositionLat() != null
                    ? convertSemicirclesToDegrees(mesg.getPositionLat())
                    : null;

            Double longitude = mesg.getPositionLong() != null
                    ? convertSemicirclesToDegrees(mesg.getPositionLong())
                    : null;

            Integer heartRate = mesg.getHeartRate() != null
                    ? mesg.getHeartRate().intValue()
                    : null;

            // Prefer enhanced speed (higher precision); fall back to standard speed
            Double speedMs = mesg.getEnhancedSpeed() != null
                    ? mesg.getEnhancedSpeed()
                    : (mesg.getSpeed() != null ? mesg.getSpeed().doubleValue() : null);

            Integer paceSecondsPerKm = speedMs != null && speedMs > 0
                    ? (int) (1000.0 / speedMs)
                    : null;

            // Prefer enhanced_altitude (field 78, higher precision) — modern Garmin devices write here.
            // Fall back to standard altitude (field 2) for older devices.
            Double altitude = mesg.getEnhancedAltitude() != null
                    ? mesg.getEnhancedAltitude()
                    : (mesg.getAltitude() != null ? mesg.getAltitude().doubleValue() : null);

            // FIT cadence = strides/min (one foot) — multiply by 2 for total steps/min (SPM)
            Integer cadence = mesg.getCadence() != null
                    ? mesg.getCadence().intValue() * 2
                    : null;

            Integer power = mesg.getPower() != null
                    ? mesg.getPower()
                    : null;

            Double distance = mesg.getDistance() != null
                    ? mesg.getDistance().doubleValue()
                    : null;

            samples.add(new ActivitySample(
                    timestamp, latitude, longitude, heartRate,
                    paceSecondsPerKm, altitude, cadence,
                    speedMs, power, distance
            ));
        }

        public ParsedFitData build() {
            if (startedAt == null || samples.isEmpty()) {
                throw new IllegalArgumentException(
                        "Invalid FIT file: missing start time or no samples"
                );
            }

            return new ParsedFitData(
                    startedAt,
                    durationSeconds != null ? durationSeconds : 0,
                    distanceMeters != null ? distanceMeters : BigDecimal.ZERO,
                    sessionInfo,
                    laps,
                    samples
            );
        }

        private Instant convertFitTimestamp(DateTime fitDateTime) {
            // FIT timestamps are seconds since 1989-12-31 00:00:00 UTC
            long fitEpoch = 631065600L; // seconds between Unix epoch (1970) and FIT epoch (1989-12-31)
            long epochSeconds = fitDateTime.getTimestamp() + fitEpoch;
            return Instant.ofEpochSecond(epochSeconds);
        }

        private Double convertSemicirclesToDegrees(Integer semicircles) {
            // FIT uses semicircles (2^31 semicircles = 180 degrees)
            return semicircles * (180.0 / Math.pow(2, 31));
        }
    }
}