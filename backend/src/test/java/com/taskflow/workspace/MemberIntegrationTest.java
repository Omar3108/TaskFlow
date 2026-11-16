package com.taskflow.workspace;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberIntegrationTest extends WorkspaceTestSupport {

    private TestUser owner;
    private TestUser admin;
    private TestUser otherAdmin;
    private TestUser member;
    private UUID workspaceId;

    @BeforeEach
    void createTeam() throws Exception {
        owner = registerUser("Owner");
        admin = registerUser("Admin");
        otherAdmin = registerUser("Other Admin");
        member = registerUser("Member");
        workspaceId = createWorkspace(owner, "Team");
        addMember(owner, workspaceId, admin, WorkspaceRole.ADMIN);
        addMember(owner, workspaceId, otherAdmin, WorkspaceRole.ADMIN);
        addMember(owner, workspaceId, member, WorkspaceRole.MEMBER);
    }

    @Test
    void anyMemberCanListMembersWithoutSensitiveData() throws Exception {
        perform(member, get(membersUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].email")
                        .value(containsInAnyOrder(owner.email(), admin.email(), otherAdmin.email(), member.email())))
                .andExpect(jsonPath("$[?(@.userId == '%s')].role", owner.id()).value(hasItem("OWNER")))
                .andExpect(jsonPath("$[?(@.userId == '%s')].displayName", member.id()).value(hasItem("Member")))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    // ---------- changing roles ----------

    @Test
    void ownerCanPromoteAndDemote() throws Exception {
        perform(owner, patch(memberUrl(member)), Map.of("role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        perform(owner, patch(memberUrl(admin)), Map.of("role", "MEMBER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));
    }

    @Test
    void onlyTheOwnerCanChangeRoles() throws Exception {
        perform(admin, patch(memberUrl(member)), Map.of("role", "ADMIN")).andExpect(status().isForbidden());
        perform(member, patch(memberUrl(admin)), Map.of("role", "MEMBER")).andExpect(status().isForbidden());
    }

    @Test
    void ownerRoleCanNeitherBeAssignedNorChanged() throws Exception {
        perform(owner, patch(memberUrl(admin)), Map.of("role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The OWNER role cannot be assigned"));

        perform(owner, patch(memberUrl(owner)), Map.of("role", "MEMBER"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The owner's role cannot be changed"));
    }

    @Test
    void unknownRoleOrMemberIsRejected() throws Exception {
        perform(owner, patch(memberUrl(member)), Map.of("role", "SUPERUSER")).andExpect(status().isBadRequest());

        perform(owner, patch(membersUrl() + "/" + UUID.randomUUID()), Map.of("role", "ADMIN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Member not found"));
    }

    // ---------- removing members ----------

    @Test
    void ownerCanRemoveAnyoneElse() throws Exception {
        perform(owner, delete(memberUrl(admin))).andExpect(status().isNoContent());
        perform(owner, delete(memberUrl(member))).andExpect(status().isNoContent());

        perform(owner, get(membersUrl())).andExpect(jsonPath("$.length()").value(2));
        // A removed member immediately loses access.
        perform(member, get("/api/v1/workspaces/{id}", workspaceId)).andExpect(status().isNotFound());
    }

    @Test
    void adminCanRemoveMembersButNotAdminsOrTheOwner() throws Exception {
        perform(admin, delete(memberUrl(otherAdmin)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("You cannot remove a member with the ADMIN role"));
        perform(admin, delete(memberUrl(owner))).andExpect(status().isForbidden());

        perform(admin, delete(memberUrl(member))).andExpect(status().isNoContent());
    }

    @Test
    void plainMemberCannotRemoveOthers() throws Exception {
        perform(member, delete(memberUrl(admin))).andExpect(status().isForbidden());
    }

    @Test
    void membersAndAdminsCanLeaveButTheOwnerCannot() throws Exception {
        perform(member, delete(memberUrl(member))).andExpect(status().isNoContent());
        perform(admin, delete(memberUrl(admin))).andExpect(status().isNoContent());

        perform(owner, delete(memberUrl(owner)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The owner cannot leave the workspace; delete it instead"));

        perform(owner, get(membersUrl())).andExpect(jsonPath("$.length()").value(2));
    }

    private String membersUrl() {
        return "/api/v1/workspaces/" + workspaceId + "/members";
    }

    private String memberUrl(TestUser user) {
        return membersUrl() + "/" + user.id();
    }
}
