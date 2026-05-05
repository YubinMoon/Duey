package com.terry.duey.auth;

import java.util.Optional;

public interface GoogleTokenVerifier {
    Optional<GoogleUser> verify(String idToken);
}
