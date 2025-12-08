package com.runalitycs.metrics_engine.config;

import com.runalitycs.metrics_engine.dto.ActivityNormalizedDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.function.BiFunction;

@Configuration
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    @Value("${runalytics.kafka.topics.normalized}.dlq")
    private String dlqTopic;

    /**
     * Configura el manejo de errores con Dead Letter Queue (DLQ)
     *
     * Estrategia:
     * 1. Reintenta 3 veces con backoff de 2 segundos
     * 2. Si sigue fallando, envía el mensaje a DLQ
     * 3. Los errores no recuperables (validación) van directo a DLQ
     */
    @Bean
    public CommonErrorHandler errorHandler(
            KafkaTemplate<String, ActivityNormalizedDto> kafkaTemplate
    ) {
        log.info("🔧 Configurando error handler con DLQ: {}", dlqTopic);

        // Recoverer: decide qué hacer con mensajes que fallan
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                destinationResolver()
        );

        // DefaultErrorHandler con retry logic
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(2000L, 3L) // 2s entre reintentos, max 3 reintentos
        );

        // Excepciones que NO deben reintentar (van directo a DLQ)
        errorHandler.addNotRetryableExceptions(
                IllegalArgumentException.class,
                NullPointerException.class,
                org.springframework.kafka.support.serializer.DeserializationException.class
        );

        // Log de cada intento
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            log.warn("⚠️  Retry attempt {} for record: {} - Error: {}",
                    deliveryAttempt,
                    record.key(),
                    ex.getMessage()
            );
        });

        log.info("✅ Error handler configurado correctamente");
        return errorHandler;
    }

    /**
     * Determina a qué tópico DLQ enviar el mensaje fallido
     */
    private BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver() {
        return (record, ex) -> {
            log.error("❌ Mensaje enviado a DLQ: topic={}, key={}, error={}",
                    record.topic(),
                    record.key(),
                    ex.getMessage()
            );

            // Todos los mensajes fallidos van al mismo DLQ
            return new TopicPartition(dlqTopic, 0);
        };
    }
}