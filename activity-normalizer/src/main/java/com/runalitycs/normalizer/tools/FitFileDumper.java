package com.runalitycs.normalizer.tools;

import com.garmin.fit.*;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Volcado COMPLETO de archivos FIT sin filtros.
 * Muestra TODOS los mensajes, TODOS los campos
 */
public class FitFileDumper {

    public static void main(String[] args) throws Exception {
        System.out.println("═".repeat(100));
        System.out.println("FIT FILE DUMPER - Volcado completo sin filtros");
        System.out.println("═".repeat(100));

        // Selecciona el archivo a analizar
        String fitFile = "fit-files/21148349838_ACTIVITY";

        System.out.println("\n📁 ARCHIVO: " + fitFile);
        System.out.println("═".repeat(100));

        dumpFitFile(fitFile);
    }

    private static void dumpFitFile(String filePath) throws Exception {
        InputStream stream = new ClassPathResource(filePath).getInputStream();
        Decode decode = new Decode();
        MesgBroadcaster broadcaster = new MesgBroadcaster(decode);

        // Contador de mensajes por tipo
        Map<String, Integer> messageCount = new LinkedHashMap<>();
        Map<String, List<String>> messageSamples = new LinkedHashMap<>();

        // Listener universal que captura TODOS los mensajes
        broadcaster.addListener(new MesgListener() {
            @Override
            public void onMesg(Mesg mesg) {
                String mesgName = mesg.getName();
                messageCount.merge(mesgName, 1, Integer::sum);

                // Guardar ejemplos de los primeros mensajes de cada tipo
                messageSamples.putIfAbsent(mesgName, new ArrayList<>());
                List<String> samples = messageSamples.get(mesgName);

                if (samples.size() < 3) {  // Guardar máximo 3 ejemplos
                    samples.add(dumpMessage(mesg));
                }
            }
        });

        // Parsear archivo
        decode.read(stream, broadcaster);

        // Mostrar resumen
        System.out.println("\n📊 RESUMEN DE MENSAJES");
        System.out.println("─".repeat(100));
        System.out.printf("%-30s %10s%n", "Tipo de Mensaje", "Cantidad");
        System.out.println("─".repeat(100));

        messageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("%-30s %10d%n", e.getKey(), e.getValue()));

        // Mostrar detalles de cada tipo de mensaje
        System.out.println("\n\n📋 DETALLE DE MENSAJES");
        System.out.println("═".repeat(100));

        for (Map.Entry<String, List<String>> entry : messageSamples.entrySet()) {
            System.out.println("\n▼ " + entry.getKey().toUpperCase());
            System.out.println("─".repeat(100));
            System.out.println("Total de mensajes: " + messageCount.get(entry.getKey()));
            System.out.println("Mostrando primeros " + entry.getValue().size() + " ejemplos:\n");

            for (int i = 0; i < entry.getValue().size(); i++) {
                System.out.println("Ejemplo #" + (i + 1) + ":");
                System.out.println(entry.getValue().get(i));
                System.out.println();
            }
        }
    }

    /**
     * Convierte un mensaje FIT en texto legible con TODOS sus campos.
     */
    private static String dumpMessage(Mesg mesg) {
        StringBuilder sb = new StringBuilder();

        // Obtener todos los campos del mensaje
        for (Field field : mesg.getFields()) {
            String fieldName = field.getName();
            Object value = field.getValue();
            String units = field.getUnits();

            // Formatear el valor según el tipo
            String formattedValue = formatValue(fieldName, value);

            sb.append(String.format("  %-30s: %s", fieldName, formattedValue));

            if (units != null && !units.isEmpty()) {
                sb.append(" ").append(units);
            }

            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * Formatea valores especiales (timestamps, posiciones, etc.)
     */
    private static String formatValue(String fieldName, Object value) {
        if (value == null) {
            return "null";
        }

        // Timestamps
        if (value instanceof DateTime) {
            return convertTimestamp((DateTime) value);
        }

        // Posiciones (semicircles a grados)
        if (fieldName.contains("position") && value instanceof Integer) {
            Integer semicircles = (Integer) value;
            double degrees = semicircles * (180.0 / Math.pow(2, 31));
            return String.format("%.6f° (%d semicircles)", degrees, semicircles);
        }

        // Arrays
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }

        // Valores normales
        return value.toString();
    }

    private static String convertTimestamp(DateTime fitDateTime) {
        if (fitDateTime == null) return "null";
        long fitEpoch = 631065600L;
        long epochSeconds = fitDateTime.getTimestamp() + fitEpoch;
        return Instant.ofEpochSecond(epochSeconds).toString() +
                " (" + fitDateTime.getTimestamp() + " FIT timestamp)";
    }
}