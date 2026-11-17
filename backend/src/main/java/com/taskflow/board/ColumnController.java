package com.taskflow.board;

import com.taskflow.board.dto.ColumnRequest;
import com.taskflow.board.dto.ColumnResponse;
import com.taskflow.board.dto.MoveColumnRequest;
import com.taskflow.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Columns")
public class ColumnController {

    private final ColumnService columnService;

    public ColumnController(ColumnService columnService) {
        this.columnService = columnService;
    }

    @PostMapping("/boards/{boardId}/columns")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a column at the end of a board (admin or owner)")
    public ColumnResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID boardId,
            @Valid @RequestBody ColumnRequest request) {
        return columnService.create(boardId, user.id(), request);
    }

    @PatchMapping("/columns/{columnId}")
    @Operation(summary = "Rename a column (admin or owner)")
    public ColumnResponse rename(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID columnId,
            @Valid @RequestBody ColumnRequest request) {
        return columnService.rename(columnId, user.id(), request);
    }

    @PatchMapping("/columns/{columnId}/move")
    @Operation(summary = "Move a column between two neighbours of the same board (admin or owner)")
    public ColumnResponse move(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID columnId,
            @Valid @RequestBody MoveColumnRequest request) {
        return columnService.move(columnId, user.id(), request);
    }

    @DeleteMapping("/columns/{columnId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a column and its cards (admin or owner)")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID columnId) {
        columnService.delete(columnId, user.id());
    }
}
