package com.terry.duey.ai;

import java.time.LocalDate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("debug")
public class DebugScheduleAiProvider implements ScheduleAiProvider {
    @Override
    public ParsedScheduleDraft parseAudio(byte[] audioBytes, String mimeType) {
        LocalDate today = LocalDate.now();
        return new ParsedScheduleDraft(
                "Debug voice schedule",
                "Created by debug AI provider",
                "Debug",
                today.toString(),
                today.toString());
    }
}
