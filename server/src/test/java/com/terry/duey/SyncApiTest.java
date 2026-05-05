package com.terry.duey;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApiTestConfig.class)
class SyncApiTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcClient jdbcClient;

    @BeforeEach
    void cleanDatabase() {
        jdbcClient.sql("DELETE FROM recurring_templates").update();
        jdbcClient.sql("DELETE FROM todos").update();
        jdbcClient.sql("DELETE FROM categories").update();
        jdbcClient.sql("DELETE FROM users").update();
    }

    @Test
    void syncApi_rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/sync/v1/bootstrap"))
                .andExpect(status().isForbidden());
    }

    @Test
    void pushPersistsAndBootstrapReturnsUserRecords() throws Exception {
        String token = accessToken("valid-google-token");

        mockMvc.perform(post("/api/sync/v1/push")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categories": [
                                    {
                                      "id": "category-1",
                                      "name": "학교",
                                      "sortOrder": 0,
                                      "createdAt": "2026-05-05T00:00:00Z",
                                      "updatedAt": "2026-05-05T00:00:00Z"
                                    }
                                  ],
                                  "todos": [
                                    {
                                      "id": "todo-1",
                                      "title": "과제",
                                      "description": "",
                                      "categoryId": "category-1",
                                      "startDate": "2026-05-05",
                                      "endDate": "2026-05-06",
                                      "completed": false,
                                      "createdAt": "2026-05-05T00:00:00Z",
                                      "updatedAt": "2026-05-05T00:00:00Z"
                                    }
                                  ],
                                  "recurringTemplates": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].name").value("학교"))
                .andExpect(jsonPath("$.todos[0].title").value("과제"));

        mockMvc.perform(get("/api/sync/v1/bootstrap")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].id").value("category-1"))
                .andExpect(jsonPath("$.todos[0].id").value("todo-1"));
    }

    @Test
    void bootstrapOnlyReturnsAuthenticatedUsersRecords() throws Exception {
        String firstUserToken = accessToken("valid-google-token");
        pushCategory(firstUserToken, "category-1", "학교");

        jdbcClient.sql("""
                        INSERT INTO users (id, google_subject, email, name, created_at, updated_at)
                        VALUES ('other-user', 'other-subject', 'other@example.com', 'Other', '2026-05-05T00:00:00Z', '2026-05-05T00:00:00Z')
                        """).update();
        jdbcClient.sql("""
                        INSERT INTO categories (id, user_id, name, sort_order, created_at, updated_at)
                        VALUES ('other-category', 'other-user', '개인', 0, '2026-05-05T00:00:00Z', '2026-05-05T00:00:00Z')
                        """).update();

        mockMvc.perform(get("/api/sync/v1/bootstrap")
                        .header("Authorization", "Bearer " + firstUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].name").value("학교"));
    }

    private void pushCategory(String token, String id, String name) throws Exception {
        mockMvc.perform(post("/api/sync/v1/push")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categories": [
                                    {
                                      "id": "%s",
                                      "name": "%s",
                                      "sortOrder": 0,
                                      "createdAt": "2026-05-05T00:00:00Z",
                                      "updatedAt": "2026-05-05T00:00:00Z"
                                    }
                                  ],
                                  "todos": [],
                                  "recurringTemplates": []
                                }
                                """.formatted(id, name)))
                .andExpect(status().isOk());
    }

    private String accessToken(String idToken) throws Exception {
        String response = mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"" + idToken + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.split("\"accessToken\":\"")[1].split("\"")[0];
    }
}
