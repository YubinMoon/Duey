package com.terry.duey;

import com.terry.duey.ai.ParsedScheduleDraft;
import com.terry.duey.ai.ScheduleAiProvider;
import com.terry.duey.auth.GoogleTokenVerifier;
import com.terry.duey.auth.GoogleUser;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@TestConfiguration
public class ApiTestConfig {
    @Bean
    @Primary
    GoogleTokenVerifier googleTokenVerifier() {
        return idToken -> "valid-google-token".equals(idToken)
                ? Optional.of(new GoogleUser("google-subject-1", "test@example.com", "Test User"))
                : Optional.empty();
    }

    @Bean
    @Primary
    FakeScheduleAiProvider fakeScheduleAiProvider() {
        return new FakeScheduleAiProvider();
    }

    public static class FakeScheduleAiProvider implements ScheduleAiProvider {
        private final AtomicReference<ParsedScheduleDraft> nextResponse = new AtomicReference<>(
                new ParsedScheduleDraft("시험", "설명", "학교", "2026-05-05", "2026-05-05")
        );
        private final AtomicReference<RuntimeException> nextFailure = new AtomicReference<>();

        @Override
        public ParsedScheduleDraft parseAudio(byte[] audioBytes, String mimeType) {
            RuntimeException failure = nextFailure.getAndSet(null);
            if (failure != null) {
                throw failure;
            }
            return nextResponse.get();
        }

        public void failNextProviderResponse() {
            nextFailure.set(new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response could not be parsed"));
        }
    }
}
