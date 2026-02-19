package com.runalytics.normalizer.tools;

import com.garmin.fit.*;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Full dump of FIT files without filters.
 * Displays ALL messages and ALL fields.
 */
public class FitFileDumper {

    private static final long FIT_EPOCH_OFFSET_SECONDS = 631065600L;

    public static void main(String[] args) throws Exception {
        System.out.println("═".repeat(100));
        System.out.println("FIT FILE DUMPER - Full dump without filters");
        System.out.println("═".repeat(100));

        // Select the file to analyze
        String fitFile = "fit-files/21148349838_ACTIVITY";

        System.out.println("\nFILE: " + fitFile);
        System.out.println("═".repeat(100));

        dumpFitFile(fitFile);
    }

    private static void dumpFitFile(String filePath) throws Exception {
        InputStream stream = new ClassPathResource(filePath).getInputStream();
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

        // Message count by type
        Map<String, Integer> messageCount = new LinkedHashMap<>();
        Map<String, List<String>> messageSamples = new LinkedHashMap<>();

        // Universal listener that captures ALL messages
        broadcaster.addListener(new MesgListener() {
            @Override
            public void onMesg(Mesg mesg) {
                String mesgName = mesg.getName();
                messageCount.merge(mesgName, 1, Integer::sum);

                // Store samples for the first messages of each type
                messageSamples.putIfAbsent(mesgName, new ArrayList<>());
                List<String> samples = messageSamples.get(mesgName);

                if (samples.size() < 3) {  // Store at most 3 examples
                    samples.add(dumpMessage(mesg));
                }
            }
        });

        // Parse file
        decode.read(stream, broadcaster);

        // Display summary
        System.out.println("\nMESSAGE SUMMARY");
        System.out.println("─".repeat(100));
        System.out.printf(Locale.US, "%-30s %10s%n", "Message Type", "Count");
        System.out.println("─".repeat(100));

        messageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf(Locale.US, "%-30s %10d%n", e.getKey(), e.getValue()));

        // Display details for each message type
        System.out.println("\n\nMESSAGE DETAILS");
        System.out.println("═".repeat(100));

        for (Map.Entry<String, List<String>> entry : messageSamples.entrySet()) {
            System.out.println("\n" + entry.getKey().toUpperCase());
            System.out.println("─".repeat(100));
            System.out.println("Total messages: " + messageCount.get(entry.getKey()));
            System.out.println("Showing first " + entry.getValue().size() + " examples:\n");

            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.println("Example #" + (i + 1) + ":");
                System.out.println(entry.getValue().get(i));
                System.out.println();
            }
        }
    }

    /**
     * Converts a FIT message to human-readable text with ALL its fields.
     */
    private static String dumpMessage(Mesg mesg) {
        StringBuilder sb = new StringBuilder();

        // Iterate over all fields in the message
        for (Field field : mesg.getFields()) {
            String fieldName = field.getName();
            Object value = field.getValue();
            String units = field.getUnits();

            String formattedValue = formatValue(fieldName, value);

            sb.append(String.format(Locale.US, "  %-30s: %s", fieldName, formattedValue));

            if (units != null && !units.isEmpty()) {
                sb.append(" ").append(units);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Formats special values (timestamps, positions, etc.)
     */
    private static String formatValue(String fieldName, Object value) {
        if (value == null) {
            return "null";
        }

        // Timestamps
        if (value instanceof DateTime) {
            return convertTimestamp((DateTime) value);
        }

        // Positions (semicircles to degrees)
        if (fieldName.contains("position") && value instanceof Integer) {
            Integer semicircles = (Integer) value;
            double degrees = semicircles * (180.0 / Math.pow(2, 31));
            return String.format(Locale.US, "%.6f° (%d semicircles)", degrees, semicircles);
        }

        // Arrays
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }

        return value.toString();
    }

    private static String convertTimestamp(DateTime fitDateTime) {
        if (fitDateTime == null) return "null";
        long epochSeconds = fitDateTime.getTimestamp() + FIT_EPOCH_OFFSET_SECONDS;
        return Instant.ofEpochSecond(epochSeconds).toString() +
                " (" + fitDateTime.getTimestamp() + " FIT timestamp)";
    }
}