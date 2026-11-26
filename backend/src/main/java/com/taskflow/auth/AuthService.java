package com.taskflow.auth;

import com.taskflow.auth.dto.AuthResponse;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.UnauthorizedException;
import com.taskflow.security.JwtService;
import com.taskflow.user.User;
import com.taskflow.user.UserMapper;
import com.taskflow.user.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;
    /** Hash compared against when the e-mail is unknown, so both login failures cost the same time. */
    private final String dummyPasswordHash;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userMapper = userMapper;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    /** Creates the account and logs the user in. */
    @Transactional
    public AuthResult register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this e-mail already exists");
        }

        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()), request.displayName().trim()));
        return createSession(user, refreshTokenService.issue(user));
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(normalizeEmail(request.email()));

        // Always run BCrypt, even for an unknown e-mail: otherwise the faster response would
        // tell an attacker which e-mails are registered (timing attack).
        String hashToCheck = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hashToCheck);

        if (found.isEmpty() || !passwordMatches) {
            // Same message for both cases, for the same reason.
            throw new UnauthorizedException("Invalid e-mail or password");
        }

        User user = found.get();
        return createSession(user, refreshTokenService.issue(user));
    }

    /** Signs in a user created by the server itself (the demo sandbox). */
    @Transactional
    public AuthResult startSession(User user) {
        return createSession(user, refreshTokenService.issue(user));
    }

    // noRollbackFor: the reuse-detection revocation done inside rotate() must be committed
    // even though the exception propagates through this transaction.
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResult refresh(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
        return createSession(rotation.user(), rotation.newRawToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private AuthResult createSession(User user, String rawRefreshToken) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        AuthResponse response = new AuthResponse(
                accessToken, jwtService.getAccessTokenTtl().toSeconds(), userMapper.toResponse(user));
        return new AuthResult(response, rawRefreshToken);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param response     body returned to the client
     * @param refreshToken raw refresh token; the controller puts it in an HttpOnly cookie and
     *                     never in the body
     */
    public record AuthResult(AuthResponse response, String refreshToken) {
    }
}
