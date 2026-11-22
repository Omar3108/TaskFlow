package com.taskflow.label;

import com.taskflow.label.dto.LabelRequest;
import com.taskflow.label.dto.LabelResponse;
import com.taskflow.security.AuthenticatedUser;
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
@RequestMapping("/api/v1")
@Tag(name = "Labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping("/boards/{boardId}/labels")
    @Operation(summary = "List the labels of a board, alphabetically")
    public List<LabelResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID boardId) {
        return labelService.list(boardId, user.id());
    }

    @PostMapping("/boards/{boardId}/labels")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a label (admin or owner); names are unique per board, ignoring case")
    public LabelResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID boardId,
            @Valid @RequestBody LabelRequest request) {
        return labelService.create(boardId, user.id(), request);
    }

    @PatchMapping("/labels/{labelId}")
    @Operation(summary = "Rename or recolour a label (admin or owner)")
    public LabelResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID labelId,
            @Valid @RequestBody LabelRequest request) {
        return labelService.update(labelId, user.id(), request);
    }

    @DeleteMapping("/labels/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a label and remove it from every card (admin or owner)")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID labelId) {
        labelService.delete(labelId, user.id());
    }
}
