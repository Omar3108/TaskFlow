package com.taskflow.workspace;

import com.taskflow.security.AuthenticatedUser;
import com.taskflow.workspace.dto.MemberResponse;
import com.taskflow.workspace.dto.UpdateMemberRoleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/members")
@Tag(name = "Workspace members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "List the members of a workspace")
    public List<MemberResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workspaceId) {
        return memberService.list(workspaceId, user.id());
    }

    @PatchMapping("/{userId}")
    @Operation(summary = "Change a member's role (owner only)")
    public MemberResponse changeRole(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateMemberRoleRequest request) {
        return memberService.changeRole(workspaceId, user.id(), userId, request.role());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a member, or leave the workspace when userId is the caller")
    public void remove(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @PathVariable UUID userId) {
        memberService.remove(workspaceId, user.id(), userId);
    }
}
