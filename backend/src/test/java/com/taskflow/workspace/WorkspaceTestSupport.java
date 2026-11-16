package com.taskflow.workspace;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.ApiIntegrationTest;
import java.util.Map;
import java.util.UUID;

/** Shared steps for workspace tests, all performed through the public API. */
abstract class WorkspaceTestSupport extends ApiIntegrationTest {

    /** A sent invitation: its id and the raw token that only the creation response reveals. */
    record SentInvitation(UUID id, String token) {
    }

    protected UUID createWorkspace(TestUser owner, String name) throws Exception {
        JsonNode body = body(perform(owner, post("/api/v1/workspaces"), Map.of("name", name))
                .andExpect(status().isCreated()));
        return UUID.fromString(body.get("id").asText());
    }

    protected SentInvitation invite(TestUser inviter, UUID workspaceId, String email, WorkspaceRole role)
            throws Exception {
        JsonNode body = body(perform(
                        inviter,
                        post("/api/v1/workspaces/{id}/invitations", workspaceId),
                        Map.of("email", email, "role", role.name()))
                .andExpect(status().isCreated()));
        return new SentInvitation(UUID.fromString(body.at("/invitation/id").asText()), body.get("token").asText());
    }

    /** Invites the user with the given role and accepts on their behalf. */
    protected void addMember(TestUser owner, UUID workspaceId, TestUser newMember, WorkspaceRole role)
            throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, newMember.email(), role);
        perform(newMember, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isOk());
    }
}
