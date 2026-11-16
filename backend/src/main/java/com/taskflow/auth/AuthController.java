package com.taskflow.auth;

import com.taskflow.auth.AuthService.AuthResult;
import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@SecurityRequirements // these endpoints are public: no Bearer token in the OpenAPI docs
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    public AuthController(AuthService authService, RefreshTokenCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/register")
    @Operation(summary = "Create an account and start a session")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return sessionResponse(HttpStatus.CREATED, authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Start a session with e-mail and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return sessionResponse(HttpStatus.OK, authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange the refresh-token cookie for a new access token (rotates the cookie)")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        return sessionResponse(HttpStatus.OK, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @Operation(summary = "End the session: revokes the refresh token and clears the cookie")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenCookieFactory.COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieFactory.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(HttpStatus status, AuthResult result) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }
}
