package com.terry.duey.ai;

import java.io.IOException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final ScheduleAiProvider scheduleAiProvider;

    public AiController(ScheduleAiProvider scheduleAiProvider) {
        this.scheduleAiProvider = scheduleAiProvider;
    }

    @PostMapping("/schedule/voice")
    public ParsedScheduleDraft parseVoice(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam("mimeType") String mimeType
    ) throws IOException {
        return scheduleAiProvider.parseAudio(audio.getBytes(), mimeType);
    }
}
