package com.taskflow.workspace;

import com.taskflow.common.error.ForbiddenException;
import com.taskflow.common.error.ResourceNotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The single place that answers "may this user act on this workspace?".
 *
 * <p>Every service method that touches workspace data starts with one of these checks. Boards
 * and cards resolve their workspace first and then come here too, so the rule lives in one
 * class instead of being repeated across the code base.
 */
@Service
public class WorkspaceAccessService {

    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAccessService(WorkspaceMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    /**
     * @return the caller's membership
     * @throws ResourceNotFoundException if the workspace does not exist <em>or</em> the caller
     *         is not a member. Both cases look identical (404) so that outsiders cannot find
     *         out which workspace ids exist.
     */
    @Transactional(readOnly = true)
    public WorkspaceMember requireMember(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found"));
    }

    /**
     * @throws ForbiddenException if the caller is a member but with a lower role. A member
     *         already knows the workspace exists, so an honest 403 leaks nothing.
     */
    @Transactional(readOnly = true)
    public WorkspaceMember requireRole(UUID workspaceId, UUID userId, WorkspaceRole minimumRole) {
        WorkspaceMember member = requireMember(workspaceId, userId);
        if (!member.getRole().isAtLeast(minimumRole)) {
            throw new ForbiddenException("This action requires the " + minimumRole + " role");
        }
        return member;
    }
}
