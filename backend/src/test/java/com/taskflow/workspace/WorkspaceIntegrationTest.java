package com.taskflow.workspace;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkspaceIntegrationTest extends WorkspaceTestSupport {

    @Test
    void creatorBecomesTheOwner() throws Exception {
        TestUser ada = registerUser("Ada");

        perform(ada, post("/api/v1/workspaces"), Map.of("name", "  Engineering  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Engineering"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void nameIsRequired() throws Exception {
        TestUser ada = registerUser("Ada");

        perform(ada, post("/api/v1/workspaces"), Map.of("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces")).andExpect(status().isUnauthorized());
    }

    @Test
    void listContainsOnlyTheCallersWorkspacesWithTheirRole() throws Exception {
        TestUser ada = registerUser("Ada");
        TestUser grace = registerUser("Grace");
        UUID adasWorkspace = createWorkspace(ada, "Ada's team");
        UUID gracesWorkspace = createWorkspace(grace, "Grace's team");
        addMember(grace, gracesWorkspace, ada, WorkspaceRole.MEMBER);

        perform(ada, get("/api/v1/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == '%s')].role", adasWorkspace).value(hasItem("OWNER")))
                .andExpect(jsonPath("$[?(@.id == '%s')].role", gracesWorkspace).value(hasItem("MEMBER")));

        perform(grace, get("/api/v1/workspaces"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].id").value(not(hasItem(adasWorkspace.toString()))));
    }

    @Test
    void outsidersGetNotFoundForEveryOperation() throws Exception {
        TestUser ada = registerUser("Ada");
        TestUser mallory = registerUser("Mallory");
        UUID workspaceId = createWorkspace(ada, "Private");

        // 404, not 403: an outsider must not learn that this id exists.
        perform(mallory, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());
        perform(mallory, patch("/api/v1/workspaces/{id}", workspaceId), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());
        perform(mallory, get("/api/v1/workspaces/{id}/members", workspaceId)).andExpect(status().isNotFound());
        perform(mallory, get("/api/v1/workspaces/{id}/invitations", workspaceId)).andExpect(status().isNotFound());

        // Same answer as for an id that really does not exist.
        perform(mallory, get("/api/v1/workspaces/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void onlyTheOwnerCanRenameOrDelete() throws Exception {
        TestUser owner = registerUser("Owner");
        TestUser admin = registerUser("Admin");
        TestUser member = registerUser("Member");
        UUID workspaceId = createWorkspace(owner, "Team");
        addMember(owner, workspaceId, admin, WorkspaceRole.ADMIN);
        addMember(owner, workspaceId, member, WorkspaceRole.MEMBER);

        for (TestUser notOwner : new TestUser[] {admin, member}) {
            perform(notOwner, patch("/api/v1/workspaces/{id}", workspaceId), Map.of("name", "Nope"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("This action requires the OWNER role"));
            perform(notOwner, delete("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isForbidden());
        }

        perform(owner, patch("/api/v1/workspaces/{id}", workspaceId), Map.of("name", "Renamed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
        perform(member, get("/api/v1/workspaces/{id}", workspaceId))
                .andExpect(jsonPath("$.name").value("Renamed"))
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void deletingAWorkspaceRemovesItForEveryMember() throws Exception {
        TestUser owner = registerUser("Owner");
        TestUser member = registerUser("Member");
        TestUser invited = registerUser("Invited");
        UUID workspaceId = createWorkspace(owner, "Short-lived");
        addMember(owner, workspaceId, member, WorkspaceRole.MEMBER);
        invite(owner, workspaceId, invited.email(), WorkspaceRole.MEMBER);

        perform(owner, delete("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNoContent());

        perform(owner, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());
        perform(member, get("/api/v1/workspaces")).andExpect(jsonPath("$.length()").value(0));
        // Pending invitations disappear with the workspace.
        perform(invited, get("/api/v1/users/me/invitations")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void malformedIdIsABadRequest() throws Exception {
        TestUser ada = registerUser("Ada");

        perform(ada, get("/api/v1/workspaces/not-a-uuid")).andExpect(status().isBadRequest());
    }
}
