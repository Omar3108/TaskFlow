package com.taskflow.demo;

import com.taskflow.auth.AuthService;
import com.taskflow.auth.AuthService.AuthResult;
import com.taskflow.auth.RefreshTokenCookieFactory;
import com.taskflow.auth.dto.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class DemoController {

    private final DemoService demoService;
    private final AuthService authService;
    private final RefreshTokenCookieFactory cookieFactory;

    public DemoController(DemoService demoService, AuthService authService, RefreshTokenCookieFactory cookieFactory) {
        this.demoService = demoService;
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/demo")
    @Operation(summary = "Create a private, pre-filled demo sandbox and sign in to it (expires after 24 hours)")
    public ResponseEntity<AuthResponse> startDemo(HttpServletRequest request) {
        // getRemoteAddr() is the real client: forward-headers-strategy=framework applies the
        // X-Forwarded-For header set by the hosting proxies.
        AuthResult result = authService.startSession(demoService.createSandbox(request.getRemoteAddr()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookieFactory.create(result.refreshToken()).toString())
                .body(result.response());
    }
}
