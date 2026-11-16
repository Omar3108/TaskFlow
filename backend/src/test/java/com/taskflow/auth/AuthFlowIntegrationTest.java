package com.taskflow.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.AbstractIntegrationTest;
import com.taskflow.user.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "s3cret-password";
    private static final String COOKIE = RefreshTokenCookieFactory.COOKIE_NAME;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;

    // ---------- register ----------

    @Test
    void registerCreatesTheUserAndStartsASession() throws Exception {
        String email = uniqueEmail();

        MvcResult result = register(email, PASSWORD, "Ada Lovelace")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.displayName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                // Neither the password hash nor the refresh token may appear in the body.
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/v1/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(7 * 24 * 3600);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("SameSite=Strict");

        // The password is stored as a BCrypt hash, never in clear text.
        String storedHash = userRepository.findByEmail(email).orElseThrow().getPasswordHash();
        assertThat(storedHash).startsWith("$2").isNotEqualTo(PASSWORD);
    }

    @Test
    void registerRejectsADuplicateEmailIgnoringCase() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Ada").andExpect(status().isCreated());

        register(email.toUpperCase(), PASSWORD, "Someone Else")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("An account with this e-mail already exists"));
    }

    @Test
    void registerReportsEveryInvalidField() throws Exception {
        register("not-an-email", "short", "")
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.errors.displayName").isNotEmpty());
    }

    @Test
    void malformedJsonIsABadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ---------- login ----------

    @Test
    void loginReturnsTokensForValidCredentials() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Ada").andExpect(status().isCreated());

        MvcResult result = login(email, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        assertThat(result.getResponse().getCookie(COOKIE)).isNotNull();
    }

    @Test
    void loginFailsTheSameWayForAWrongPasswordAndAnUnknownEmail() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD, "Ada").andExpect(status().isCreated());

        login(email, "wrong-password")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid e-mail or password"));

        login(uniqueEmail(), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid e-mail or password"));
    }

    // ---------- protected routes ----------

    @Test
    void protectedRouteRequiresAToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void protectedRouteRejectsAnInvalidToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileCanBeReadAndUpdatedWithAValidToken() throws Exception {
        String email = uniqueEmail();
        String accessToken = accessTokenOf(register(email, PASSWORD, "Ada").andReturn());

        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Ada"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", "  Ada Lovelace  "))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Ada Lovelace"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("displayName", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.displayName").isNotEmpty());
    }

    // ---------- refresh ----------

    @Test
    void refreshRotatesTheTokenAndIssuesANewAccessToken() throws Exception {
        Cookie original = refreshCookieOf(register(uniqueEmail(), PASSWORD, "Ada").andReturn());

        MvcResult refreshed = refresh(original)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        Cookie rotated = refreshCookieOf(refreshed);
        assertThat(rotated.getValue()).isNotEqualTo(original.getValue());

        // The new access token works.
        mockMvc.perform(get("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, bearer(accessTokenOf(refreshed))))
                .andExpect(status().isOk());
    }

    @Test
    void reusingARotatedTokenRevokesEverySessionOfTheUser() throws Exception {
        Cookie original = refreshCookieOf(register(uniqueEmail(), PASSWORD, "Ada").andReturn());
        Cookie rotated = refreshCookieOf(refresh(original).andExpect(status().isOk()).andReturn());

        // The original token was already used once: presenting it again looks like theft.
        refresh(original).andExpect(status().isUnauthorized());

        // ...so even the newest, otherwise valid token is now revoked.
        refresh(rotated).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutACookieOrWithAnUnknownTokenIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        refresh(new Cookie(COOKIE, "unknown-token")).andExpect(status().isUnauthorized());
    }

    // ---------- logout ----------

    @Test
    void logoutRevokesTheRefreshTokenAndClearsTheCookie() throws Exception {
        Cookie cookie = refreshCookieOf(register(uniqueEmail(), PASSWORD, "Ada").andReturn());

        MvcResult result = mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie cleared = result.getResponse().getCookie(COOKIE);
        assertThat(cleared).isNotNull();
        assertThat(cleared.getValue()).isEmpty();
        assertThat(cleared.getMaxAge()).isZero();

        refresh(cookie).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutASessionStillSucceeds() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    // ---------- helpers ----------

    private ResultActions register(String email, String password, String displayName) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password, "displayName", displayName))));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("email", email, "password", password))));
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }

    private String accessTokenOf(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("accessToken").asText();
    }

    private Cookie refreshCookieOf(MvcResult result) {
        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        // Send back only name and value, as a browser would.
        return new Cookie(cookie.getName(), cookie.getValue());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
