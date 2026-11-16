package com.taskflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.taskflow.auth.AuthService.AuthResult;
import com.taskflow.auth.dto.LoginRequest;
import com.taskflow.auth.dto.RegisterRequest;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.UnauthorizedException;
import com.taskflow.security.JwtService;
import com.taskflow.user.User;
import com.taskflow.user.UserMapper;
import com.taskflow.user.UserRepository;
import com.taskflow.user.dto.UserResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String DUMMY_HASH = "dummy-hash";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private UserMapper userMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // The constructor pre-computes the hash used for unknown e-mails.
        when(passwordEncoder.encode("dummy-password-for-timing")).thenReturn(DUMMY_HASH);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService, userMapper);
    }

    @Test
    void registerNormalizesEmailHashesPasswordAndStartsASession() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("s3cret-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(refreshTokenService.issue(any(User.class))).thenReturn("raw-refresh-token");
        stubTokenGeneration();

        AuthResult result = authService.register(
                new RegisterRequest("  Ada@Example.COM ", "s3cret-password", "  Ada Lovelace "));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Ada Lovelace");

        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().expiresIn()).isEqualTo(900);
    }

    @Test
    void registerRejectsAnEmailThatIsAlreadyTaken() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ada@example.com", "s3cret-password", "Ada")))
                .isInstanceOf(ConflictException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginSucceedsWithTheRightPassword() {
        User user = new User("ada@example.com", "bcrypt-hash", "Ada");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("s3cret-password", "bcrypt-hash")).thenReturn(true);
        when(refreshTokenService.issue(user)).thenReturn("raw-refresh-token");
        stubTokenGeneration();

        AuthResult result = authService.login(new LoginRequest("ADA@example.com", "s3cret-password"));

        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.response().accessToken()).isEqualTo("access-token");
    }

    @Test
    void loginFailsWithTheWrongPassword() {
        User user = new User("ada@example.com", "bcrypt-hash", "Ada");
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "bcrypt-hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid e-mail or password");

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void loginWithUnknownEmailStillChecksAPasswordAndFailsWithTheSameMessage() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
        // Even if the dummy hash happened to match, an unknown e-mail must never log in.
        when(passwordEncoder.matches("any-password", DUMMY_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "any-password")))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Invalid e-mail or password");

        // BCrypt ran anyway, so the response time does not reveal that the e-mail is unknown.
        verify(passwordEncoder).matches("any-password", DUMMY_HASH);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refreshWithoutATokenIsRejected() {
        assertThatThrownBy(() -> authService.refresh(null)).isInstanceOf(InvalidRefreshTokenException.class);
        assertThatThrownBy(() -> authService.refresh("  ")).isInstanceOf(InvalidRefreshTokenException.class);

        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void logoutWithoutATokenDoesNothing() {
        authService.logout(null);

        verifyNoInteractions(refreshTokenService);
    }

    private void stubTokenGeneration() {
        when(jwtService.generateAccessToken(any(), anyString())).thenReturn("access-token");
        when(jwtService.getAccessTokenTtl()).thenReturn(Duration.ofMinutes(15));
        when(userMapper.toResponse(any(User.class))).thenReturn(
                new UserResponse(UUID.randomUUID(), "ada@example.com", "Ada", Instant.now()));
    }
}
