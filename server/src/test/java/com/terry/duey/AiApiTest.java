package com.terry.duey;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiTestConfig.class)
class AiApiTest {
    @Autowired MockMvc mockMvc;

    @Autowired JdbcClient jdbcClient;

    @Autowired ApiTestConfig.FakeScheduleAiProvider aiProvider;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM recurring_templates").update();
        jdbcClient.sql("DELETE FROM todos").update();
        jdbcClient.sql("DELETE FROM categories").update();
        jdbcClient.sql("DELETE FROM users").update();
    }

    @Test
    void voiceApi_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(
                        multipart("/api/ai/schedule/voice")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "voice.m4a",
                                                "audio/mp4",
                                                "audio".getBytes()))
                                .param("mimeType", "audio/mp4"))
                .andExpect(status().isForbidden());
    }

    @Test
    void voiceApi_returnsParsedDraftForAuthenticatedUser() throws Exception {
        String token = accessToken();

        mockMvc.perform(
                        multipart("/api/ai/schedule/voice")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "voice.m4a",
                                                "audio/mp4",
                                                "audio".getBytes()))
                                .param("mimeType", "audio/mp4")
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("시험"))
                .andExpect(jsonPath("$.startDate").value("2026-05-05"));
    }

    @Test
    void voiceApi_returnsControlledErrorWhenProviderFails() throws Exception {
        aiProvider.failNextProviderResponse();

        mockMvc.perform(
                        multipart("/api/ai/schedule/voice")
                                .file(
                                        new MockMultipartFile(
                                                "audio",
                                                "voice.m4a",
                                                "audio/mp4",
                                                "audio".getBytes()))
                                .param("mimeType", "audio/mp4")
                                .header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isBadGateway());
    }

    private String accessToken() throws Exception {
        String response =
                mockMvc.perform(
                                post("/api/auth/google")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"idToken\":\"valid-google-token\"}"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        return response.split("\"accessToken\":\"")[1].split("\"")[0];
    }
}
