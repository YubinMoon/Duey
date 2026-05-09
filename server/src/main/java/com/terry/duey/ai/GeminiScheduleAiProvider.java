package com.terry.duey.ai;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type.Known;
import com.terry.duey.config.DueyProperties;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiScheduleAiProvider implements ScheduleAiProvider {
    private final DueyProperties properties;
    private final JsonMapper jsonMapper;

    public GeminiScheduleAiProvider(DueyProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public ParsedScheduleDraft parseAudio(byte[] audioBytes, String mimeType) {
        if (audioBytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audio file is empty");
        }
        if (!"gemini".equalsIgnoreCase(properties.ai().provider())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "AI provider is not configured");
        }
        if (properties.ai().geminiApiKey() == null || properties.ai().geminiApiKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Gemini API key is not configured");
        }
        GenerateContentResponse response;
        try (Client client = Client.builder().apiKey(properties.ai().geminiApiKey()).build()) {
            response = client.models.generateContent(properties.ai().geminiModel(),
                    geminiContent(audioBytes, mimeType), geminiConfig());
        }

        String text = response == null ? "" : response.text();
        return parseDraft(text);
    }

    private Content geminiContent(byte[] audioBytes, String mimeType) {
        return Content.fromParts(Part.fromBytes(audioBytes, mimeType));
    }

    private GenerateContentConfig geminiConfig() {
        Schema schema = Schema.builder().type(Known.OBJECT)
                .properties(Map.of("title", stringSchema(), "description", stringSchema(),
                        "category", stringSchema(), "start_date", dateSchema(), "end_date",
                        dateSchema()))
                .required(List.of("title", "start_date", "end_date"))
                .propertyOrdering(
                        List.of("title", "description", "category", "start_date", "end_date"))
                .build();
        return GenerateContentConfig.builder()
                .systemInstruction(
                        Content.fromParts(Part.fromText("유저 입력을 기반으로 Todo 리스트 등록을 하십시오")))
                .responseMimeType("application/json").candidateCount(1).responseSchema(schema)
                .build();
    }

    private Schema stringSchema() {
        return Schema.builder().type(Known.STRING).build();
    }

    private Schema dateSchema() {
        return Schema.builder().type(Known.STRING).format("date").build();
    }

    private ParsedScheduleDraft parseDraft(String rawText) {
        try {
            JsonNode json = jsonMapper.readTree(rawText.trim());
            String title = json.path("title").asString("").trim();
            if (title.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI response did not include a title");
            }
            LocalDate start = parseDate(json.path("start_date").asString(null), LocalDate.now());
            LocalDate end = parseDate(json.path("end_date").asString(null), start);
            if (end.isBefore(start)) {
                end = start;
            }
            return new ParsedScheduleDraft(title, json.path("description").asString("").trim(),
                    json.path("category").asString("").trim(), start.toString(), end.toString());
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI response could not be parsed", exception);
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
