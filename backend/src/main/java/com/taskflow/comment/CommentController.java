package com.taskflow.comment;

import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.common.web.PageResponse;
import com.taskflow.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@Tag(name = "Comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @GetMapping("/cards/{cardId}/comments")
    @Operation(summary = "List the comments of a card, newest first (page, size; sort only by createdAt)")
    public PageResponse<CommentResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID cardId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return commentService.list(cardId, user.id(), pageable);
    }

    @PostMapping("/cards/{cardId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Comment on a card")
    public CommentResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody CommentRequest request) {
        return commentService.create(cardId, user.id(), request);
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "Edit a comment (author only)")
    public CommentResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request) {
        return commentService.update(commentId, user.id(), request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment (author, or an admin as moderation)")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID commentId) {
        commentService.delete(commentId, user.id());
    }
}
