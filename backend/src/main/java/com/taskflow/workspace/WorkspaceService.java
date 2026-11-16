package com.taskflow.workspace;

import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import com.taskflow.workspace.dto.WorkspaceRequest;
import com.taskflow.workspace.dto.WorkspaceResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService accessService;
    private final WorkspaceMapper mapper;

    public WorkspaceService(
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceAccessService accessService,
            WorkspaceMapper mapper) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.mapper = mapper;
    }

    /** Creates the workspace and makes the caller its owner, atomically. */
    @Transactional
    public WorkspaceResponse create(UUID userId, WorkspaceRequest request) {
        Workspace workspace = workspaceRepository.save(new Workspace(request.name().trim()));
        // getReferenceById returns a proxy holding only the id: enough to set the foreign key
        // without running a SELECT on users.
        User creator = userRepository.getReferenceById(userId);
        WorkspaceMember owner = memberRepository.save(new WorkspaceMember(workspace, creator, WorkspaceRole.OWNER));
        return mapper.toWorkspaceResponse(owner);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceResponse> listForUser(UUID userId) {
        return memberRepository.findAllByUserIdWithWorkspace(userId).stream()
                .map(mapper::toWorkspaceResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceResponse get(UUID workspaceId, UUID userId) {
        return mapper.toWorkspaceResponse(accessService.requireMember(workspaceId, userId));
    }

    @Transactional
    public WorkspaceResponse rename(UUID workspaceId, UUID userId, WorkspaceRequest request) {
        WorkspaceMember owner = accessService.requireRole(workspaceId, userId, WorkspaceRole.OWNER);
        owner.getWorkspace().setName(request.name().trim());
        return mapper.toWorkspaceResponse(owner);
    }

    /** Members, invitations (and later boards) are removed by ON DELETE CASCADE in the database. */
    @Transactional
    public void delete(UUID workspaceId, UUID userId) {
        WorkspaceMember owner = accessService.requireRole(workspaceId, userId, WorkspaceRole.OWNER);
        workspaceRepository.delete(owner.getWorkspace());
    }
}
