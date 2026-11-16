package com.taskflow.workspace;

import com.taskflow.security.AuthenticatedUser;
import com.taskflow.workspace.dto.CreateInvitationRequest;
import com.taskflow.workspace.dto.InvitationCreatedResponse;
import com.taskflow.workspace.dto.InvitationResponse;
import com.taskflow.workspace.dto.ReceivedInvitationResponse;
import com.taskflow.workspace.dto.WorkspaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Invitations")
public class InvitationController {

    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    // ---------- workspace admins ----------

    @GetMapping("/workspaces/{workspaceId}/invitations")
    @Operation(summary = "List the pending invitations of a workspace (admins)")
    public List<InvitationResponse> listPending(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workspaceId) {
        return invitationService.listPending(workspaceId, user.id());
    }

    @PostMapping("/workspaces/{workspaceId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Invite someone by e-mail (admins); the token is returned only once")
    public InvitationCreatedResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateInvitationRequest request) {
        return invitationService.create(workspaceId, user.id(), request);
    }

    @DeleteMapping("/workspaces/{workspaceId}/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke a pending invitation (admins)")
    public void revoke(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @PathVariable UUID invitationId) {
        invitationService.revoke(workspaceId, user.id(), invitationId);
    }

    // ---------- invitees: by link ----------

    @GetMapping("/invitations/{token}")
    @Operation(summary = "Preview the invitation behind a link")
    public ReceivedInvitationResponse preview(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String token) {
        return invitationService.preview(token, user);
    }

    @PostMapping("/invitations/{token}/accept")
    @Operation(summary = "Accept the invitation behind a link")
    public WorkspaceResponse acceptByToken(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable String token) {
        return invitationService.acceptByToken(token, user);
    }

    // ---------- invitees: from the in-app list ----------

    @GetMapping("/users/me/invitations")
    @Operation(summary = "List the invitations waiting for the caller")
    public List<ReceivedInvitationResponse> listReceived(@AuthenticationPrincipal AuthenticatedUser user) {
        return invitationService.listReceived(user);
    }

    @PostMapping("/users/me/invitations/{invitationId}/accept")
    @Operation(summary = "Accept one of the caller's invitations")
    public WorkspaceResponse acceptById(
            @AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID invitationId) {
        return invitationService.acceptById(invitationId, user);
    }

    @PostMapping("/users/me/invitations/{invitationId}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Decline one of the caller's invitations")
    public void declineById(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID invitationId) {
        invitationService.declineById(invitationId, user);
    }
}
