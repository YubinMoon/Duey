package com.terry.duey.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleAuthRequest request) {
        return authService.loginWithGoogle(request.idToken());
    }

    public record GoogleAuthRequest(@NotBlank String idToken) {}

    public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {}

    public record UserResponse(String id, String email, String name) {}
}
