package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runalytics.ai_coach.config.OpenAiConfig;
import com.runalytics.ai_coach.dto.openai.OpenAiRequest;
import com.runalytics.ai_coach.dto.openai.OpenAiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiApiService {

    private final WebClient openAiWebClient;
    private final OpenAiConfig config;
    private final ObjectMapper objectMapper;

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    /**
     * Analyze training data using OpenAI GPT-4o
     *
     * @param systemPrompt The system prompt (role definition)
     * @param userPrompt The user prompt (actual data to analyze)
     * @return JSON string with recommendations
     */
    public String analyze(String systemPrompt, String userPrompt) {
        log.info("Calling OpenAI API with model: {}", config.getModel());

        // Build request
        OpenAiRequest request = buildRequest(systemPrompt, userPrompt);

        try {
            // Call OpenAI API
            OpenAiResponse response = openAiWebClient
                    .post()
                    .uri("/chat/completions")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OpenAiResponse.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new IllegalStateException("Empty response from OpenAI API");
            }

            // Extract content from first choice
            String content = response.getChoices().get(0).getMessage().getContent();

            log.info("OpenAI API call successful. Tokens used: {}",
                    response.getUsage() != null ? response.getUsage().getTotalTokens() : "unknown");

            return content;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to call OpenAI API", e);
        }
    }

    /**
     * Build OpenAI request with configured parameters
     */
    private OpenAiRequest buildRequest(String systemPrompt, String userPrompt) {
        return OpenAiRequest.builder()
                .model(config.getModel())
                .maxTokens(config.getMaxTokens())
                .temperature(config.getTemperature())
                .messages(List.of(
                        OpenAiRequest.Message.builder()
                                .role("system")
                                .content(systemPrompt)
                                .build(),
                        OpenAiRequest.Message.builder()
                                .role("user")
                                .content(userPrompt)
                                .build()
                ))
                .responseFormat(OpenAiRequest.ResponseFormat.builder()
                        .type("json_object")
                        .build())
                .build();
    }
}
