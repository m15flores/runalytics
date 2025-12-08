package com.runalitycs.metrics_engine.kafka;

import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import com.runalitycs.metrics_engine.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class MetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricsConsumer.class);

    private final MetricsService metricsService;

    public MetricsConsumer(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @KafkaListener(
            topics = "activities.normalized",
            groupId = "metrics-engine-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(@Payload ActivityNormalizedDto message,
                       @Header(KafkaHeaders.RECEIVED_KEY) String key,
                       org.springframework.kafka.support.Acknowledgment acknowledgment) {
        log.info("Received activity from Kafka: {}", message.activityId());

        try {
            validateActivity(message);
            metricsService.processActivity(message);
            log.info("Mensaje procesado exitosamente: {}", key);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            // Errores de validación → NO reintentar
            log.error("Error de validación (NO se reintentará): {}", key, e);
            acknowledgment.acknowledge(); // Skip este mensaje

        } catch (DataAccessException e) {
            // Error de BD → Reintentar (puede ser temporal)
            log.error("Error de BD (se reintentará): {}", key, e);
            throw e; // Spring Kafka reintentará automáticamente

        } catch (Exception e) {
            // Error desconocido → Log detallado y reintentar
            log.error("Error inesperado procesando actividad: {}", key, e);
            log.error("Stacktrace completo:", e);
            throw e; // Reintentar
        }
    }

    /**
     * Validación básica del mensaje
     */
    private void validateActivity(ActivityNormalizedDto activity) {
        if (activity == null) {
            throw new IllegalArgumentException("Activity cannot be null");
        }
        if (activity.activityId() == null) {
            throw new IllegalArgumentException("ActivityId cannot be null");
        }
        if (activity.userId() == null || activity.userId().isBlank()) {
            throw new IllegalArgumentException("UserId cannot be null or empty");
        }
        if (activity.session() == null) {
            throw new IllegalArgumentException("SessionData cannot be null");
        }

        log.debug("Validación básica OK para activity: {}", activity.activityId());
    }
}