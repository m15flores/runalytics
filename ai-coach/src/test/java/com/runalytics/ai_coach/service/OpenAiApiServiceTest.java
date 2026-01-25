package com.runalytics.ai_coach.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.runalytics.ai_coach.config.OpenAiConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OpenAiApiServiceTest {

    private WireMockServer wireMockServer;
    private OpenAiApiService openAiApiService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Start WireMock server
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Create ObjectMapper
        objectMapper = new ObjectMapper();

        // Create WebClient pointing to WireMock
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:8089")
                .defaultHeader("Authorization", "Bearer test-api-key")
                .defaultHeader("Content-Type", "application/json")
                .build();

        // Create OpenAiConfig mock
        OpenAiConfig config = new OpenAiConfig() {
            @Override
            public String getModel() {
                return "gpt-4o";
            }

            @Override
            public Integer getMaxTokens() {
                return 2000;
            }

            @Override
            public Double getTemperature() {
                return 0.7;
            }
        };

        // Create service
        openAiApiService = new OpenAiApiService(webClient, config, objectMapper);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldCallOpenAiApiSuccessfully() {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze this training data: 50km this week.";

        String mockResponse = """
                {
                  "id": "chatcmpl-123",
                  "object": "chat.completion",
                  "created": 1677652288,
                  "model": "gpt-4o",
                  "choices": [{
                    "index": 0,
                    "message": {
                      "role": "assistant",
                      "content": "{\\"recommendations\\": [{\\"type\\": \\"TRAINING_VOLUME\\", \\"priority\\": \\"HIGH\\", \\"content\\": \\"Good volume\\", \\"rationale\\": \\"On track\\"}]}"
                    },
                    "finish_reason": "stop"
                  }],
                  "usage": {
                    "prompt_tokens": 50,
                    "completion_tokens": 30,
                    "total_tokens": 80
                  }
                }
                """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        String result = openAiApiService.analyze(systemPrompt, userPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("recommendations");
        assertThat(result).contains("TRAINING_VOLUME");
        assertThat(result).contains("Good volume");

        // Verify request was made
        verify(postRequestedFor(urlEqualTo("/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-api-key"))
                .withHeader("Content-Type", equalTo("application/json")));
    }

    @Test
    void shouldHandleApiErrorsGracefully() {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze this data.";

        String errorResponse = """
            {
              "error": {
                "message": "Invalid API key",
                "type": "invalid_request_error",
                "code": "invalid_api_key"
              }
            }
            """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(errorResponse)));

        // When & Then
        assertThatThrownBy(() -> openAiApiService.analyze(systemPrompt, userPrompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call OpenAI API");
    }

    @Test
    void shouldHandleTimeout() {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze this data.";

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")
                        .withFixedDelay(35000))); // 35 seconds delay (exceeds 30s timeout)

        // When & Then
        assertThatThrownBy(() -> openAiApiService.analyze(systemPrompt, userPrompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call OpenAI API");
    }
    @Test
    void shouldSendCorrectRequestStructure() throws Exception {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze: 50km this week, avg pace 5:00 min/km";

        String mockResponse = """
            {
              "id": "chatcmpl-123",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-4o",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "{\\"recommendations\\": []}"
                },
                "finish_reason": "stop"
              }],
              "usage": {
                "prompt_tokens": 50,
                "completion_tokens": 10,
                "total_tokens": 60
              }
            }
            """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        openAiApiService.analyze(systemPrompt, userPrompt);

        // Then - Verify request body structure
        verify(postRequestedFor(urlEqualTo("/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("gpt-4o")))
                .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("2000")))
                .withRequestBody(matchingJsonPath("$.temperature", equalTo("0.7")))
                .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
                .withRequestBody(matchingJsonPath("$.messages[0].content", equalTo(systemPrompt)))
                .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
                .withRequestBody(matchingJsonPath("$.messages[1].content", equalTo(userPrompt)))
                .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_object"))));
    }

    @Test
    void shouldParseJsonResponseCorrectly() {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze this training.";

        String mockResponse = """
            {
              "id": "chatcmpl-456",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-4o",
              "choices": [{
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "{\\"recommendations\\": [{\\"type\\": \\"RECOVERY\\", \\"priority\\": \\"HIGH\\", \\"content\\": \\"Take rest\\", \\"rationale\\": \\"Overtraining\\"}]}"
                },
                "finish_reason": "stop"
              }],
              "usage": {
                "prompt_tokens": 100,
                "completion_tokens": 50,
                "total_tokens": 150
              }
            }
            """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When
        String result = openAiApiService.analyze(systemPrompt, userPrompt);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).contains("\"type\": \"RECOVERY\"");
        assertThat(result).contains("\"priority\": \"HIGH\"");
        assertThat(result).contains("\"content\": \"Take rest\"");
        assertThat(result).contains("\"rationale\": \"Overtraining\"");
    }

    @Test
    void shouldHandleEmptyChoices() {
        // Given
        String systemPrompt = "You are an expert running coach.";
        String userPrompt = "Analyze this data.";

        String mockResponse = """
            {
              "id": "chatcmpl-789",
              "object": "chat.completion",
              "created": 1677652288,
              "model": "gpt-4o",
              "choices": [],
              "usage": {
                "prompt_tokens": 10,
                "completion_tokens": 0,
                "total_tokens": 10
              }
            }
            """;

        stubFor(post(urlEqualTo("/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // When & Then
        assertThatThrownBy(() -> openAiApiService.analyze(systemPrompt, userPrompt))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call OpenAI API")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

}
