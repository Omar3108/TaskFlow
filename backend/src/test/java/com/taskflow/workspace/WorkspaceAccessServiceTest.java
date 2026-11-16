package com.taskflow.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.taskflow.common.error.ForbiddenException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.user.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceAccessServiceTest {

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();

    @Mock
    private WorkspaceMemberRepository memberRepository;

    @InjectMocks
    private WorkspaceAccessService accessService;

    @Test
    void nonMemberGetsNotFound() {
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accessService.requireMember(workspaceId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Workspace not found");
        // Role checks on a non-member must also answer 404, never 403.
        assertThatThrownBy(() -> accessService.requireRole(workspaceId, userId, WorkspaceRole.MEMBER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void memberIsReturned() {
        WorkspaceMember member = memberWithRole(WorkspaceRole.MEMBER);

        assertThat(accessService.requireMember(workspaceId, userId)).isSameAs(member);
    }

    @ParameterizedTest(name = "{0} asking for {1} -> allowed={2}")
    @CsvSource({
        "OWNER,  OWNER,  true",
        "OWNER,  ADMIN,  true",
        "OWNER,  MEMBER, true",
        "ADMIN,  OWNER,  false",
        "ADMIN,  ADMIN,  true",
        "ADMIN,  MEMBER, true",
        "MEMBER, OWNER,  false",
        "MEMBER, ADMIN,  false",
        "MEMBER, MEMBER, true",
    })
    void roleHierarchyIsEnforced(WorkspaceRole actual, WorkspaceRole required, boolean allowed) {
        WorkspaceMember member = memberWithRole(actual);

        if (allowed) {
            assertThat(accessService.requireRole(workspaceId, userId, required)).isSameAs(member);
        } else {
            assertThatThrownBy(() -> accessService.requireRole(workspaceId, userId, required))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessage("This action requires the " + required + " role");
        }
    }

    private WorkspaceMember memberWithRole(WorkspaceRole role) {
        WorkspaceMember member =
                new WorkspaceMember(new Workspace("Team"), new User("ada@example.com", "hash", "Ada"), role);
        when(memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(member));
        return member;
    }
}
