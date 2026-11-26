package com.taskflow.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.ApiIntegrationTest;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class DemoIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DemoCleanup cleanup;
    @Autowired
    private JdbcTemplate jdbc;

    /** Each test uses its own client address, so the per-address limit never leaks between tests. */
    private static String freshAddress() {
        UUID random = UUID.randomUUID();
        return "10." + (random.getMostSignificantBits() & 0xff) + "." + (random.getLeastSignificantBits() & 0xff)
                + "." + (Math.abs(random.hashCode()) % 250 + 1);
    }

    private ResultActions startDemo(String address) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/demo").with(request -> {
            request.setRemoteAddr(address);
            return request;
        }));
    }

    private TestUser demoVisitor(String address) throws Exception {
        JsonNode body = body(startDemo(address).andExpect(status().isCreated()));
        return new TestUser(
                UUID.fromString(body.at("/user/id").asText()),
                body.at("/user/email").asText(),
                body.at("/user/displayName").asText(),
                body.get("accessToken").asText());
    }

    @Test
    void startsASignedInSessionLikeALogin() throws Exception {
        startDemo(freshAddress())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.user.displayName").value("Demo Visitor"))
                .andExpect(jsonPath("$.user.email").value(endsWith("@demo.taskflow.dev")))
                // The refresh token travels in the same HttpOnly cookie as after a login.
                .andExpect(header().string("Set-Cookie", containsString("refresh_token=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    void theSandboxIsPrefilledAndPrivate() throws Exception {
        TestUser visitor = demoVisitor(freshAddress());
        TestUser otherVisitor = demoVisitor(freshAddress());

        JsonNode workspaces = body(perform(visitor, get("/api/v1/workspaces")).andExpect(status().isOk()));
        assertThat(workspaces).hasSize(1);
        assertThat(workspaces.get(0).get("name").asText()).isEqualTo("Demo workspace");
        assertThat(workspaces.get(0).get("role").asText()).isEqualTo("OWNER");
        String workspaceId = workspaces.get(0).get("id").asText();

        perform(visitor, get("/api/v1/workspaces/{id}/members", workspaceId))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].displayName").value("Sam Rivera"));

        String boardId = body(perform(visitor, get("/api/v1/workspaces/{id}/boards", workspaceId)))
                .get(0).get("id").asText();
        JsonNode board = body(perform(visitor, get("/api/v1/boards/{id}", boardId)).andExpect(status().isOk()));
        assertThat(board.get("columns")).hasSize(3);
        assertThat(board.at("/columns/0/cards")).hasSize(3);
        assertThat(board.at("/columns/0/cards/0/commentCount").asLong()).isEqualTo(2);
        assertThat(board.at("/columns/0/cards/1/labels/0/name").asText()).isEqualTo("Bug");
        perform(visitor, get("/api/v1/boards/{id}/labels", boardId)).andExpect(jsonPath("$.length()").value(4));

        // Another visitor's sandbox is invisible: same 404 as for any foreign board.
        perform(otherVisitor, get("/api/v1/boards/{id}", boardId)).andExpect(status().isNotFound());
    }

    @Test
    void eachAddressCanOnlyStartAFewDemosPerHour() throws Exception {
        String address = freshAddress();
        for (int i = 0; i < 5; i++) {
            startDemo(address).andExpect(status().isCreated());
        }

        startDemo(address)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Too many demos from this address. Please try again in an hour."));
        // Other visitors are not affected.
        startDemo(freshAddress()).andExpect(status().isCreated());
    }

    @Test
    void expiredSandboxesAreDeletedWithEverythingTheyOwn() throws Exception {
        TestUser visitor = demoVisitor(freshAddress());
        String workspaceId = body(perform(visitor, get("/api/v1/workspaces"))).get(0).get("id").asText();
        TestUser realUser = registerUser("Real user");

        // Make the sandbox (visitor and teammate) look expired.
        jdbc.update("update users set demo_expires_at = ? where demo_expires_at is not null and id in "
                        + "(select user_id from workspace_members where workspace_id = ?::uuid)",
                java.sql.Timestamp.from(Instant.now().minusSeconds(60)), workspaceId);

        cleanup.deleteExpired();

        assertThat(userRepository.findById(visitor.id())).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from workspaces where id = ?::uuid", Long.class, workspaceId))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "select count(*) from workspace_members where workspace_id = ?::uuid", Long.class, workspaceId))
                .isZero();
        // Real accounts are never touched.
        assertThat(userRepository.findById(realUser.id())).isPresent();
    }

    @Test
    void realAccountsAreNotDemoAccounts() throws Exception {
        TestUser realUser = registerUser("Real user");

        User stored = userRepository.findById(realUser.id()).orElseThrow();
        assertThat(stored.getDemoExpiresAt()).isNull();
    }

    @Test
    void theDeployedCommitIsPublic() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.commit").value("local"));
    }
}
