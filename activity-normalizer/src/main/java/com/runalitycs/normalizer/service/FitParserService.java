package com.runalitycs.normalizer.service;

import com.garmin.fit.*;
import com.runalitycs.normalizer.dto.ActivitySample;
import com.runalitycs.normalizer.dto.ParsedFitData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class FitParserService {

    private static final Logger log = LoggerFactory.getLogger(FitParserService.class);

    public ParsedFitData parse(InputStream fitFileStream) {
        if (fitFileStream == null) {
            throw new IllegalArgumentException("FIT file stream cannot be null");
        }

        Decode decode = new Decode();
        MesgBroadcaster mesgBroadcaster = new MesgBroadcaster(decode);

        // Variables para capturar datos
        FitDataCollector collector = new FitDataCollector();

        // Listeners para diferentes tipos de mensajes
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

        // Parsear el archivo FIT
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
     * Clase interna para recolectar datos durante el parsing.
     */
    private static class FitDataCollector {
        private Instant startedAt;
        private Integer durationSeconds;
        private BigDecimal distanceMeters;
        private final List<ActivitySample> samples = new ArrayList<>();

        public void onSession(SessionMesg mesg) {
            // Extraer metadata de la sesión
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
            // Extraer cada sample (punto de datos)
            Instant timestamp = mesg.getTimestamp() != null
                    ? convertFitTimestamp(mesg.getTimestamp())
                    : null;

            if (timestamp == null) {
                return; // Skip records sin timestamp
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
            // FIT timestamps son segundos desde 1989-12-31 00:00:00 UTC
            long fitEpoch = 631065600L; // Seconds between 1970-01-01 and 1989-12-31
            long epochSeconds = fitDateTime.getTimestamp() + fitEpoch;
            return Instant.ofEpochSecond(epochSeconds);
        }

        private Double convertSemicirclesToDegrees(Integer semicircles) {
            // FIT usa semicircles (2^31 semicircles = 180 degrees)
            return semicircles * (180.0 / Math.pow(2, 31));
        }

        private Integer calculatePaceFromSpeed(Float speedMetersPerSecond) {
            // Pace = segundos por km
            // Speed = metros por segundo
            // Pace (s/km) = 1000 / speed
            if (speedMetersPerSecond <= 0) {
                return null;
            }
            return (int) (1000.0 / speedMetersPerSecond);
        }
    }
}