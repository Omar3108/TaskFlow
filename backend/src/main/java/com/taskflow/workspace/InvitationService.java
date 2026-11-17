package com.taskflow.workspace;

import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.error.ConflictException;
import com.taskflow.common.error.ForbiddenException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.security.AuthenticatedUser;
import com.taskflow.security.SecureTokens;
import com.taskflow.user.UserRepository;
import com.taskflow.workspace.dto.CreateInvitationRequest;
import com.taskflow.workspace.dto.InvitationCreatedResponse;
import com.taskflow.workspace.dto.InvitationResponse;
import com.taskflow.workspace.dto.ReceivedInvitationResponse;
import com.taskflow.workspace.dto.WorkspaceResponse;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService accessService;
    private final WorkspaceMapper mapper;
    private final InvitationProperties properties;
    private final Clock clock;

    public InvitationService(
            InvitationRepository invitationRepository,
            WorkspaceMemberRepository memberRepository,
            UserRepository userRepository,
            WorkspaceAccessService accessService,
            WorkspaceMapper mapper,
            InvitationProperties properties,
            Clock clock) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.accessService = accessService;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    // ---------- workspace admins ----------

    @Transactional
    public InvitationCreatedResponse create(UUID workspaceId, UUID actorId, CreateInvitationRequest request) {
        WorkspaceMember actor = accessService.requireRole(workspaceId, actorId, WorkspaceRole.ADMIN);

        if (request.role() == WorkspaceRole.OWNER) {
            throw new BadRequestException("The OWNER role cannot be assigned");
        }
        // Granting ADMIN is a role decision, and only the owner makes those.
        if (request.role() == WorkspaceRole.ADMIN && actor.getRole() != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Only the owner can invite someone as ADMIN");
        }

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (memberRepository.existsByWorkspaceIdAndUserEmail(workspaceId, email)) {
            throw new ConflictException("This person is already a member of the workspace");
        }

        Instant now = clock.instant();
        invitationRepository.findByWorkspaceIdAndEmailAndStatus(workspaceId, email, InvitationStatus.PENDING)
                .ifPresent(existing -> {
                    if (!existing.isExpired(now)) {
                        throw new ConflictException("There is already a pending invitation for this e-mail");
                    }
                    // A stale invitation would block the new one (unique index on pending rows).
                    // Flush now: Hibernate otherwise runs INSERTs before UPDATEs at commit time.
                    existing.markAs(InvitationStatus.EXPIRED);
                    invitationRepository.flush();
                });

        String rawToken = SecureTokens.generate();
        Invitation invitation = invitationRepository.save(new Invitation(
                actor.getWorkspace(),
                email,
                request.role(),
                SecureTokens.hash(rawToken),
                actor.getUser(),
                now.plus(properties.ttl())));

        return new InvitationCreatedResponse(mapper.toInvitationResponse(invitation), rawToken);
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> listPending(UUID workspaceId, UUID actorId) {
        accessService.requireRole(workspaceId, actorId, WorkspaceRole.ADMIN);
        return invitationRepository.findUsableByWorkspaceId(workspaceId, clock.instant()).stream()
                .map(mapper::toInvitationResponse)
                .toList();
    }

    @Transactional
    public void revoke(UUID workspaceId, UUID actorId, UUID invitationId) {
        accessService.requireRole(workspaceId, actorId, WorkspaceRole.ADMIN);
        Invitation invitation = invitationRepository.findByIdAndWorkspaceId(invitationId, workspaceId)
                .filter(found -> found.getStatus() == InvitationStatus.PENDING)
                .orElseThrow(InvitationService::notFound);
        invitation.markAs(InvitationStatus.REVOKED);
    }

    // ---------- invitees ----------

    /** Invitations waiting for the caller, matched by the e-mail address of their account. */
    @Transactional(readOnly = true)
    public List<ReceivedInvitationResponse> listReceived(AuthenticatedUser user) {
        return invitationRepository.findUsableByEmail(user.email(), clock.instant()).stream()
                .map(mapper::toReceivedInvitationResponse)
                .toList();
    }

    /** What the invitation link shows before the user decides. */
    @Transactional(readOnly = true)
    public ReceivedInvitationResponse preview(String rawToken, AuthenticatedUser user) {
        return mapper.toReceivedInvitationResponse(findUsableByToken(rawToken, user));
    }

    @Transactional
    public WorkspaceResponse acceptByToken(String rawToken, AuthenticatedUser user) {
        return accept(findUsableByToken(rawToken, user), user);
    }

    @Transactional
    public WorkspaceResponse acceptById(UUID invitationId, AuthenticatedUser user) {
        return accept(findUsableById(invitationId, user), user);
    }

    @Transactional
    public void declineById(UUID invitationId, AuthenticatedUser user) {
        findUsableById(invitationId, user).markAs(InvitationStatus.DECLINED);
    }

    private WorkspaceResponse accept(Invitation invitation, AuthenticatedUser user) {
        Workspace workspace = invitation.getWorkspace();
        invitation.markAs(InvitationStatus.ACCEPTED);

        // Normally the invitee is not a member yet. If they already are (for example they
        // accepted an older invitation), keep the existing membership and role untouched.
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspace.getId(), user.id())
                .orElseGet(() -> memberRepository.save(new WorkspaceMember(
                        workspace, userRepository.getReferenceById(user.id()), invitation.getRole())));
        return mapper.toWorkspaceResponse(member);
    }

    private Invitation findUsableByToken(String rawToken, AuthenticatedUser user) {
        Invitation invitation = invitationRepository.findByTokenHashWithDetails(SecureTokens.hash(rawToken))
                .filter(found -> found.isUsable(clock.instant()))
                .orElseThrow(InvitationService::notFound);

        // Whoever holds the link already knows the invitation exists, so an explicit 403 leaks
        // nothing and lets the UI say "you are signed in with the wrong account".
        if (!invitation.getEmail().equals(user.email())) {
            throw new ForbiddenException("This invitation was sent to a different e-mail address");
        }
        return invitation;
    }

    private Invitation findUsableById(UUID invitationId, AuthenticatedUser user) {
        // Looked up by id AND e-mail: someone else's invitation is simply "not found".
        return invitationRepository.findByIdAndEmailWithWorkspace(invitationId, user.email())
                .filter(found -> found.isUsable(clock.instant()))
                .orElseThrow(InvitationService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        // One message for unknown, expired, revoked and already answered invitations.
        return new ResourceNotFoundException("Invitation not found or no longer valid");
    }
}
