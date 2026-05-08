package com.terry.duey;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:sqlite:file:duey_debug_test?mode=memory&cache=shared"
        })
@AutoConfigureMockMvc
@ActiveProfiles("debug")
class DebugAuthApiTest {
    @Autowired MockMvc mockMvc;

    @Test
    void debugProfileAllowsSyncWithoutBearerToken() throws Exception {
        mockMvc.perform(get("/api/sync/v1/bootstrap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(0));
    }

    @Test
    void debugProfileAllowsVoiceApiWithoutBearerToken() throws Exception {
        mockMvc.perform(
                        multipart("/api/ai/schedule/voice")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "voice.m4a",
                                                "audio/mp4",
                                                "audio".getBytes()))
                                .param("mimeType", "audio/mp4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Debug voice schedule"));
    }
}
