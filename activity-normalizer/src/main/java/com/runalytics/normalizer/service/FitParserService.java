package com.runalytics.normalizer.service;

import com.garmin.fit.*;
import com.runalytics.normalizer.dto.ActivitySample;
import com.runalytics.normalizer.dto.ParsedFitData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        private final List<ActivitySample> samples = new ArrayList<>();

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

            Integer paceSecondsPerKm = mesg.getSpeed() != null && mesg.getSpeed() > 0
                    ? calculatePaceFromSpeed(mesg.getSpeed())
                    : null;

            Double altitude = mesg.getAltitude() != null
                    ? mesg.getAltitude().doubleValue()
                    : null;

            Integer cadence = mesg.getCadence() != null
                    ? mesg.getCadence().intValue()
                    : null;

            samples.add(new ActivitySample(
                    timestamp, latitude, longitude, heartRate,
                    paceSecondsPerKm, altitude, cadence
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

        private Integer calculatePaceFromSpeed(Float speedMetersPerSecond) {
            if (speedMetersPerSecond <= 0) {
                return null;
            }
            return (int) (1000.0 / speedMetersPerSecond);
        }
    }
}