package com.taskflow.workspace;

import com.taskflow.security.AuthenticatedUser;
import com.taskflow.workspace.dto.WorkspaceRequest;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces")
@Tag(name = "Workspaces")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @GetMapping
    @Operation(summary = "List the workspaces the caller belongs to")
    public List<WorkspaceResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return workspaceService.listForUser(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a workspace; the caller becomes its owner")
    public WorkspaceResponse create(
            @AuthenticationPrincipal AuthenticatedUser user, @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.create(user.id(), request);
    }

    @GetMapping("/{workspaceId}")
    @Operation(summary = "Get a workspace (members only)")
    public WorkspaceResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workspaceId) {
        return workspaceService.get(workspaceId, user.id());
    }

    @PatchMapping("/{workspaceId}")
    @Operation(summary = "Rename a workspace (owner only)")
    public WorkspaceResponse rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody WorkspaceRequest request) {
        return workspaceService.rename(workspaceId, user.id(), request);
    }

    @DeleteMapping("/{workspaceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a workspace and everything in it (owner only)")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workspaceId) {
        workspaceService.delete(workspaceId, user.id());
    }
}
