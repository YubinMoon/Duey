package com.terry.duey.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public UserRecord upsertGoogleUser(GoogleUser googleUser) {
        Optional<UserRecord> existing = findByGoogleSubject(googleUser.subject());
        if (existing.isPresent()) {
            UserRecord user = existing.get();
            jdbcClient.sql("UPDATE users SET email = :email, name = :name, updated_at = :updatedAt WHERE id = :id")
                    .param("email", googleUser.email())
                    .param("name", googleUser.name())
                    .param("updatedAt", Instant.now().toString())
                    .param("id", user.id())
                    .update();
            return new UserRecord(user.id(), user.googleSubject(), googleUser.email(), googleUser.name());
        }

        String id = UUID.randomUUID().toString();
        String now = Instant.now().toString();
        jdbcClient.sql("""
                        INSERT INTO users (id, google_subject, email, name, created_at, updated_at)
                        VALUES (:id, :googleSubject, :email, :name, :createdAt, :updatedAt)
                        """)
                .param("id", id)
                .param("googleSubject", googleUser.subject())
                .param("email", googleUser.email())
                .param("name", googleUser.name())
                .param("createdAt", now)
                .param("updatedAt", now)
                .update();
        return new UserRecord(id, googleUser.subject(), googleUser.email(), googleUser.name());
    }

    private Optional<UserRecord> findByGoogleSubject(String googleSubject) {
        return jdbcClient.sql("SELECT id, google_subject, email, name FROM users WHERE google_subject = :googleSubject")
                .param("googleSubject", googleSubject)
                .query((rs, rowNum) -> new UserRecord(
                        rs.getString("id"),
                        rs.getString("google_subject"),
                        rs.getString("email"),
                        rs.getString("name")
                ))
                .optional();
    }
}
