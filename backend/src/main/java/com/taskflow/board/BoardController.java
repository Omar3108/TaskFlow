package com.taskflow.board;

import com.taskflow.board.dto.BoardDetailResponse;
import com.taskflow.board.dto.BoardRequest;
import com.taskflow.board.dto.BoardResponse;
import com.taskflow.board.dto.CreateBoardRequest;
import com.taskflow.card.CardFilter;
import com.taskflow.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
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

// Nested routes create and list (the parent is in the URL); flat routes operate on one board.
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Boards")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/workspaces/{workspaceId}/boards")
    @Operation(summary = "List the boards of a workspace")
    public List<BoardResponse> list(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID workspaceId) {
        return boardService.list(workspaceId, user.id());
    }

    @PostMapping("/workspaces/{workspaceId}/boards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a board (admin or owner); it starts with three columns unless defaultColumns is false")
    public BoardResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreateBoardRequest request) {
        return boardService.create(workspaceId, user.id(), request);
    }

    @GetMapping("/boards/{boardId}")
    @Operation(summary = "Get a board with its columns and cards; optional filters return only the matching cards")
    public BoardDetailResponse get(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID boardId,
            @ParameterObject @Valid CardFilter filter) {
        return boardService.get(boardId, user.id(), filter);
    }

    @PatchMapping("/boards/{boardId}")
    @Operation(summary = "Update the name and description of a board (admin or owner)")
    public BoardResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID boardId,
            @Valid @RequestBody BoardRequest request) {
        return boardService.update(boardId, user.id(), request);
    }

    @DeleteMapping("/boards/{boardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a board and everything in it (admin or owner)")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID boardId) {
        boardService.delete(boardId, user.id());
    }
}
