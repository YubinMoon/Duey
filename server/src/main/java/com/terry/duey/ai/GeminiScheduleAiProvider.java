package com.terry.duey.ai;

import com.terry.duey.config.DueyProperties;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiScheduleAiProvider implements ScheduleAiProvider {
    private final DueyProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public GeminiScheduleAiProvider(
            DueyProperties properties,
            JsonMapper jsonMapper,
            RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.restClient =
                restClientBuilder.baseUrl("https://generativelanguage.googleapis.com").build();
    }

    @Override
    public ParsedScheduleDraft parseAudio(byte[] audioBytes, String mimeType) {
        if (audioBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is empty");
        }
        if (!"gemini".equalsIgnoreCase(properties.ai().provider())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "AI provider is not configured");
        }
        if (properties.ai().geminiApiKey() == null || properties.ai().geminiApiKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Gemini API key is not configured");
        }

        JsonNode response =
                restClient
                        .post()
                        .uri(
                                "/v1beta/models/{model}:generateContent",
                                properties.ai().geminiModel())
                        .header("x-goog-api-key", properties.ai().geminiApiKey())
                        .body(geminiRequest(audioBytes, mimeType))
                        .retrieve()
                        .body(JsonNode.class);

        String text =
                response == null
                        ? ""
                        : response.path("candidates")
                                .path(0)
                                .path("content")
                                .path("parts")
                                .path(0)
                                .path("text")
                                .asString("");
        return parseDraft(text);
    }

    private Map<String, Object> geminiRequest(byte[] audioBytes, String mimeType) {
        Map<String, Object> schema =
                Map.of(
                        "type",
                        "object",
                        "properties",
                        Map.of(
                                "title",
                                Map.of("type", "string"),
                                "description",
                                Map.of("type", "string"),
                                "category",
                                Map.of("type", "string"),
                                "start_date",
                                Map.of("type", "string", "format", "date"),
                                "end_date",
                                Map.of("type", "string", "format", "date")),
                        "required",
                        List.of("title", "start_date", "end_date"),
                        "additionalProperties",
                        false);
        return Map.of(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of("text", "첨부된 한국어 음성에서 일정을 추출하세요."),
                                        Map.of(
                                                "inline_data",
                                                Map.of(
                                                        "mime_type",
                                                        mimeType,
                                                        "data",
                                                        Base64.getEncoder()
                                                                .encodeToString(audioBytes)))))),
                "generationConfig",
                Map.of("responseMimeType", "application/json", "responseJsonSchema", schema));
    }

    private ParsedScheduleDraft parseDraft(String rawText) {
        try {
            JsonNode json = jsonMapper.readTree(rawText.trim());
            String title = json.path("title").asString("").trim();
            if (title.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI response did not include a title");
            }
            LocalDate start = parseDate(json.path("start_date").asString(null), LocalDate.now());
            LocalDate end = parseDate(json.path("end_date").asString(null), start);
            if (end.isBefore(start)) {
                end = start;
            }
            return new ParsedScheduleDraft(
                    title,
                    json.path("description").asString("").trim(),
                    json.path("category").asString("").trim(),
                    start.toString(),
                    end.toString());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "AI response could not be parsed", exception);
        }
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
