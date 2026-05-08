package com.terry.duey.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(
            GoogleTokenVerifier googleTokenVerifier,
            JwtService jwtService,
            UserRepository userRepository) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public AuthController.AuthResponse loginWithGoogle(String idToken) {
        GoogleUser googleUser =
                googleTokenVerifier
                        .verify(idToken)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.UNAUTHORIZED, "Invalid Google token"));
        UserRecord user = userRepository.upsertGoogleUser(googleUser);
        return new AuthController.AuthResponse(
                jwtService.issueAccessToken(user.id()),
                jwtService.issueRefreshToken(user.id()),
                new AuthController.UserResponse(user.id(), user.email(), user.name()));
    }
}
