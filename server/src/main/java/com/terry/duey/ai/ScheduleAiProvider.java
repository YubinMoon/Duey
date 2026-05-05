package com.terry.duey.ai;

public interface ScheduleAiProvider {
    ParsedScheduleDraft parseAudio(byte[] audioBytes, String mimeType);
}
