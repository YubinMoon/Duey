package com.terry.duey.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "duey")
public record DueyProperties(Auth auth, Ai ai) {
    public record Auth(
            String googleClientId,
            String jwtSecret,
            String debugUserId,
            long accessTokenMinutes,
            long refreshTokenDays) {}

    public record Ai(String provider, String geminiApiKey, String geminiModel) {}
}
