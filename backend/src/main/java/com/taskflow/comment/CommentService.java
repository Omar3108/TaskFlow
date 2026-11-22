package com.taskflow.comment;

import com.taskflow.board.BoardAccessService;
import com.taskflow.card.CardAccessService;
import com.taskflow.card.CardAccessService.CardAccess;
import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.common.error.BadRequestException;
import com.taskflow.common.error.ForbiddenException;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.common.web.PageResponse;
import com.taskflow.user.UserRepository;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceMember;
import com.taskflow.workspace.WorkspaceRole;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments on cards. Every member can read and post. Only the author edits a comment (nobody
 * should be able to put words in someone else's mouth); the author or an admin deletes it, so
 * admins can moderate.
 */
@Service
public class CommentService {

    private static final String COMMENT_NOT_FOUND = "Comment not found";
    /** Newest first; the id breaks ties between comments posted in the same instant. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CardAccessService cardAccess;
    private final WorkspaceAccessService workspaceAccess;
    private final CommentMapper mapper;

    public CommentService(
            CommentRepository commentRepository,
            UserRepository userRepository,
            CardAccessService cardAccess,
            WorkspaceAccessService workspaceAccess,
            CommentMapper mapper) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.cardAccess = cardAccess;
        this.workspaceAccess = workspaceAccess;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> list(UUID cardId, UUID userId, Pageable pageable) {
        cardAccess.requireCard(cardId, userId);
        return PageResponse.of(commentRepository.findByCardId(cardId, withAllowedSort(pageable)), mapper::toResponse);
    }

    @Transactional
    public CommentResponse create(UUID cardId, UUID userId, CommentRequest request) {
        CardAccess access = cardAccess.requireCard(cardId, userId);
        Comment comment = new Comment(access.card(), userRepository.getReferenceById(userId), request.body().trim());
        commentRepository.save(comment);
        // The proxy only holds the id; the response needs the author's name, so load the user.
        return mapper.toResponse(commentRepository.findByIdWithCard(comment.getId()).orElseThrow());
    }

    @Transactional
    public CommentResponse update(UUID commentId, UUID userId, CommentRequest request) {
        Comment comment = requireComment(commentId, userId).comment();
        if (!isAuthor(comment, userId)) {
            throw new ForbiddenException("Only the author can edit a comment");
        }
        comment.setBody(request.body().trim());
        // Flush so @PreUpdate runs and the response already says "edited".
        return mapper.toResponse(commentRepository.saveAndFlush(comment));
    }

    @Transactional
    public void delete(UUID commentId, UUID userId) {
        CommentAccess access = requireComment(commentId, userId);
        if (!isAuthor(access.comment(), userId) && !access.member().getRole().isAtLeast(WorkspaceRole.ADMIN)) {
            throw new ForbiddenException("Only the author or an admin can delete a comment");
        }
        commentRepository.delete(access.comment());
    }

    private record CommentAccess(Comment comment, WorkspaceMember member) {
    }

    private CommentAccess requireComment(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findByIdWithCard(commentId)
                .orElseThrow(() -> new ResourceNotFoundException(COMMENT_NOT_FOUND));
        WorkspaceMember member = workspaceAccess.requireRole(
                BoardAccessService.workspaceIdOf(comment.getCard().getColumn()),
                userId,
                WorkspaceRole.MEMBER,
                COMMENT_NOT_FOUND);
        return new CommentAccess(comment, member);
    }

    private static boolean isAuthor(Comment comment, UUID userId) {
        return comment.getAuthor() != null && comment.getAuthor().getId().equals(userId);
    }

    /**
     * Comments can only be sorted by creation date. Accepting any property from the URL would
     * let a client sort by fields it should not depend on, and an unknown one would fail deep
     * inside the query with a 500 instead of a clear 400.
     */
    private static Pageable withAllowedSort(Pageable pageable) {
        Sort sort = NEWEST_FIRST;
        for (Sort.Order order : pageable.getSort()) {
            if (!order.getProperty().equals("createdAt")) {
                throw new BadRequestException("Comments can only be sorted by createdAt");
            }
            sort = Sort.by(order, new Sort.Order(order.getDirection(), "id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
