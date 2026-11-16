package com.taskflow.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.security.SecureTokens;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class InvitationIntegrationTest extends WorkspaceTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private TestUser owner;
    private TestUser admin;
    private TestUser member;
    private TestUser invitee;
    private UUID workspaceId;

    @BeforeEach
    void createTeam() throws Exception {
        owner = registerUser("Owner");
        admin = registerUser("Admin");
        member = registerUser("Member");
        invitee = registerUser("Invitee");
        workspaceId = createWorkspace(owner, "Team");
        addMember(owner, workspaceId, admin, WorkspaceRole.ADMIN);
        addMember(owner, workspaceId, member, WorkspaceRole.MEMBER);
    }

    // ---------- creating ----------

    @Test
    void creationReturnsTheTokenOnceAndStoresOnlyItsHash() throws Exception {
        JsonNode body = body(perform(
                        owner, post(invitationsUrl()), Map.of("email", "New.Person@Example.com", "role", "MEMBER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.invitation.email").value("new.person@example.com"))
                .andExpect(jsonPath("$.invitation.role").value("MEMBER"))
                .andExpect(jsonPath("$.invitation.status").value("PENDING"))
                .andExpect(jsonPath("$.invitation.invitedByName").value("Owner"))
                .andExpect(jsonPath("$.invitation.expiresAt").isNotEmpty()));

        String token = body.get("token").asText();
        String storedHash = jdbcTemplate.queryForObject(
                "select token_hash from invitations where id = ?::uuid",
                String.class,
                body.at("/invitation/id").asText());
        assertThat(storedHash).isEqualTo(SecureTokens.hash(token)).isNotEqualTo(token);

        // The list never exposes the token again.
        perform(owner, get(invitationsUrl()))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].token").doesNotExist())
                .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test
    void membersCannotInviteAndAdminsCannotGrantAdmin() throws Exception {
        perform(member, post(invitationsUrl()), Map.of("email", invitee.email(), "role", "MEMBER"))
                .andExpect(status().isForbidden());
        perform(member, get(invitationsUrl())).andExpect(status().isForbidden());

        perform(admin, post(invitationsUrl()), Map.of("email", invitee.email(), "role", "ADMIN"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the owner can invite someone as ADMIN"));

        perform(admin, post(invitationsUrl()), Map.of("email", invitee.email(), "role", "MEMBER"))
                .andExpect(status().isCreated());
    }

    @Test
    void invalidRequestsAreRejected() throws Exception {
        perform(owner, post(invitationsUrl()), Map.of("email", "not-an-email", "role", "MEMBER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").isNotEmpty());

        perform(owner, post(invitationsUrl()), Map.of("email", invitee.email(), "role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The OWNER role cannot be assigned"));
    }

    @Test
    void existingMembersAndDuplicatePendingInvitationsAreConflicts() throws Exception {
        perform(owner, post(invitationsUrl()), Map.of("email", member.email().toUpperCase(), "role", "MEMBER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This person is already a member of the workspace"));

        invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        perform(owner, post(invitationsUrl()), Map.of("email", invitee.email(), "role", "MEMBER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("There is already a pending invitation for this e-mail"));
    }

    // ---------- accepting by link ----------

    @Test
    void inviteeCanPreviewAndAcceptByLink() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.ADMIN);

        perform(invitee, get("/api/v1/invitations/{token}", invitation.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workspaceId").value(workspaceId.toString()))
                .andExpect(jsonPath("$.workspaceName").value("Team"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.invitedByName").value("Owner"));

        perform(invitee, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        perform(invitee, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isOk());
        // The invitation is consumed: no longer pending, and the link cannot be replayed.
        perform(owner, get(invitationsUrl())).andExpect(jsonPath("$.length()").value(0));
        perform(invitee, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void linkOnlyWorksForTheInvitedEmail() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        TestUser someoneElse = registerUser("Someone Else");

        perform(someoneElse, get("/api/v1/invitations/{token}", invitation.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("This invitation was sent to a different e-mail address"));
        perform(someoneElse, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isForbidden());

        perform(someoneElse, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());
    }

    @Test
    void linkRequiresAuthenticationAndAValidToken() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);

        mockMvc.perform(get("/api/v1/invitations/{token}", invitation.token())).andExpect(status().isUnauthorized());
        perform(invitee, get("/api/v1/invitations/{token}", "made-up-token")).andExpect(status().isNotFound());
    }

    @Test
    void personInvitedBeforeRegisteringSeesTheInvitationAfterSigningUp() throws Exception {
        // The invitation targets an e-mail with no account yet; it is matched once that
        // person registers with the same address.
        String futureEmail = uniqueEmail();
        SentInvitation invitation = invite(owner, workspaceId, futureEmail, WorkspaceRole.MEMBER);
        TestUser future = registerUser("Future", futureEmail);

        perform(future, get("/api/v1/users/me/invitations"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(invitation.id().toString()));
    }

    // ---------- in-app list ----------

    @Test
    void inviteeCanAcceptFromTheirList() throws Exception {
        SentInvitation invitation = invite(admin, workspaceId, invitee.email(), WorkspaceRole.MEMBER);

        perform(invitee, get("/api/v1/users/me/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].workspaceName").value("Team"))
                .andExpect(jsonPath("$[0].invitedByName").value("Admin"));

        perform(invitee, post("/api/v1/users/me/invitations/{id}/accept", invitation.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        perform(invitee, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(0));
        perform(owner, get("/api/v1/workspaces/{id}/members", workspaceId))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void inviteeCanDeclineAndBeInvitedAgain() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);

        perform(invitee, post("/api/v1/users/me/invitations/{id}/decline", invitation.id()))
                .andExpect(status().isNoContent());

        perform(invitee, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(0));
        perform(invitee, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isNotFound());
        perform(invitee, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());

        // Declining does not block a later invitation.
        invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
    }

    @Test
    void nobodyCanAnswerAnInvitationAddressedToSomeoneElse() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        TestUser someoneElse = registerUser("Someone Else");

        perform(someoneElse, post("/api/v1/users/me/invitations/{id}/accept", invitation.id()))
                .andExpect(status().isNotFound());
        perform(someoneElse, post("/api/v1/users/me/invitations/{id}/decline", invitation.id()))
                .andExpect(status().isNotFound());

        // Still pending for the real invitee.
        perform(invitee, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- revoking and expiry ----------

    @Test
    void adminCanRevokeAndTheLinkStopsWorking() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);

        perform(member, delete(invitationsUrl() + "/" + invitation.id())).andExpect(status().isForbidden());
        perform(admin, delete(invitationsUrl() + "/" + invitation.id())).andExpect(status().isNoContent());

        perform(invitee, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isNotFound());
        perform(invitee, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(0));
        // Revoking twice: there is no pending invitation any more.
        perform(admin, delete(invitationsUrl() + "/" + invitation.id())).andExpect(status().isNotFound());
    }

    @Test
    void invitationCannotBeRevokedThroughAnotherWorkspace() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        TestUser otherOwner = registerUser("Other Owner");
        UUID otherWorkspace = createWorkspace(otherOwner, "Other");

        perform(otherOwner, delete("/api/v1/workspaces/{ws}/invitations/{id}", otherWorkspace, invitation.id()))
                .andExpect(status().isNotFound());

        perform(owner, get(invitationsUrl())).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void expiredInvitationIsUnusableAndCanBeReplaced() throws Exception {
        SentInvitation invitation = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        jdbcTemplate.update(
                "update invitations set expires_at = now() - interval '1 minute' where id = ?::uuid",
                invitation.id().toString());

        perform(invitee, post("/api/v1/invitations/{token}/accept", invitation.token()))
                .andExpect(status().isNotFound());
        perform(invitee, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(0));
        perform(owner, get(invitationsUrl())).andExpect(jsonPath("$.length()").value(0));

        // The stale row must not block a fresh invitation for the same e-mail.
        SentInvitation replacement = invite(owner, workspaceId, invitee.email(), WorkspaceRole.MEMBER);
        perform(invitee, post("/api/v1/invitations/{token}/accept", replacement.token()))
                .andExpect(status().isOk());

        String oldStatus = jdbcTemplate.queryForObject(
                "select status from invitations where id = ?::uuid", String.class, invitation.id().toString());
        assertThat(oldStatus).isEqualTo("EXPIRED");
    }

    private String invitationsUrl() {
        return "/api/v1/workspaces/" + workspaceId + "/invitations";
    }
}
