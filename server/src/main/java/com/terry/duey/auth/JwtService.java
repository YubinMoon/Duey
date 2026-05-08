package com.terry.duey.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.terry.duey.config.DueyProperties;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final DueyProperties properties;

    public JwtService(DueyProperties properties) {
        this.properties = properties;
    }

    public String issueAccessToken(String userId) {
        return issueToken(
                userId, Instant.now().plusSeconds(properties.auth().accessTokenMinutes() * 60));
    }

    public String issueRefreshToken(String userId) {
        return issueToken(
                userId,
                Instant.now().plusSeconds(properties.auth().refreshTokenDays() * 24 * 60 * 60));
    }

    public Optional<String> parseUserId(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!jwt.verify(new MACVerifier(secretBytes()))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            if (claims.getExpirationTime() == null
                    || claims.getExpirationTime().before(new Date())) {
                return Optional.empty();
            }
            return Optional.ofNullable(claims.getSubject());
        } catch (JOSEException | ParseException ignored) {
            return Optional.empty();
        }
    }

    private String issueToken(String userId, Instant expiresAt) {
        try {
            JWTClaimsSet claims =
                    new JWTClaimsSet.Builder()
                            .subject(userId)
                            .expirationTime(Date.from(expiresAt))
                            .issueTime(new Date())
                            .build();
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            jwt.sign(new MACSigner(secretBytes()));
            return jwt.serialize();
        } catch (JOSEException exception) {
            throw new IllegalStateException("Unable to issue token", exception);
        }
    }

    private byte[] secretBytes() {
        return properties.auth().jwtSecret().getBytes();
    }
}
