package com.terry.duey.auth;

import com.terry.duey.config.DueyProperties;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
@Profile("!test")
public class HttpGoogleTokenVerifier implements GoogleTokenVerifier {
    private final DueyProperties properties;
    private final RestClient restClient;

    public HttpGoogleTokenVerifier(DueyProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
    }

    @Override
    public Optional<GoogleUser> verify(String idToken) {
        if (properties.auth().googleClientId() == null || properties.auth().googleClientId().isBlank()) {
            return Optional.empty();
        }
        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo").queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
        if (response == null || !properties.auth().googleClientId().equals(response.path("aud").asText())) {
            return Optional.empty();
        }
        return Optional.of(new GoogleUser(
                response.path("sub").asText(),
                response.path("email").asText(),
                response.path("name").asText(null)
        ));
    }
}
