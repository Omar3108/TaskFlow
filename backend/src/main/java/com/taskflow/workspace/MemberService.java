package com.taskflow.workspace;

import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.ForbiddenException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.workspace.dto.MemberResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceAccessService accessService;
    private final WorkspaceMapper mapper;

    public MemberService(
            WorkspaceMemberRepository memberRepository, WorkspaceAccessService accessService, WorkspaceMapper mapper) {
        this.memberRepository = memberRepository;
        this.accessService = accessService;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<MemberResponse> list(UUID workspaceId, UUID userId) {
        accessService.requireMember(workspaceId, userId);
        return memberRepository.findAllByWorkspaceIdWithUser(workspaceId).stream()
                .map(mapper::toMemberResponse)
                .toList();
    }

    /** Only the owner changes roles, and only between ADMIN and MEMBER. */
    @Transactional
    public MemberResponse changeRole(UUID workspaceId, UUID actorId, UUID targetUserId, WorkspaceRole newRole) {
        accessService.requireRole(workspaceId, actorId, WorkspaceRole.OWNER);
        if (newRole == WorkspaceRole.OWNER) {
            throw new BadRequestException("The OWNER role cannot be assigned");
        }

        WorkspaceMember target = findMember(workspaceId, targetUserId);
        if (target.getRole() == WorkspaceRole.OWNER) {
            throw new ConflictException("The owner's role cannot be changed");
        }

        target.setRole(newRole);
        return mapper.toMemberResponse(target);
    }

    /**
     * Removes a member. Removing yourself means leaving the workspace.
     *
     * <ul>
     *   <li>Anyone except the owner can leave.</li>
     *   <li>The owner can remove anyone else.</li>
     *   <li>An admin can remove plain members, but not other admins or the owner.</li>
     * </ul>
     */
    @Transactional
    public void remove(UUID workspaceId, UUID actorId, UUID targetUserId) {
        WorkspaceMember actor = accessService.requireMember(workspaceId, actorId);

        if (actorId.equals(targetUserId)) {
            if (actor.getRole() == WorkspaceRole.OWNER) {
                // A workspace without an owner could never be administered again.
                throw new ConflictException("The owner cannot leave the workspace; delete it instead");
            }
            memberRepository.delete(actor);
            return;
        }

        if (!actor.getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new ForbiddenException("This action requires the ADMIN role");
        }
        WorkspaceMember target = findMember(workspaceId, targetUserId);
        // Removing someone requires strictly outranking them.
        boolean outranksTarget = actor.getRole() != target.getRole() && actor.getRole().isAtLeast(target.getRole());
        if (!outranksTarget) {
            throw new ForbiddenException("You cannot remove a member with the " + target.getRole() + " role");
        }
        memberRepository.delete(target);
    }

    private WorkspaceMember findMember(UUID workspaceId, UUID userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}
