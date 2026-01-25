package com.runalytics.ai_coach.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenAiRequest {
    private String model;

    private List<Message> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("response_format")
    private ResponseFormat responseFormat;

    @Data
    @Builder
    public static class Message {
        private String role;  // "system" or "user"
        private String content;
    }

    @Data
    @Builder
    public static class ResponseFormat {
        private String type;  // "json_object" for JSON mode
    }
}
