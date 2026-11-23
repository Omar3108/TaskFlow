package com.taskflow.demo;

import com.taskflow.board.BoardService;
import com.taskflow.board.dto.BoardDetailResponse;
import com.taskflow.board.dto.CreateBoardRequest;
import com.taskflow.card.CardFilter;
import com.taskflow.card.CardService;
import com.taskflow.card.dto.CardResponse;
import com.taskflow.card.dto.CreateCardRequest;
import com.taskflow.card.dto.UpdateCardRequest;
import com.taskflow.comment.CommentService;
import com.taskflow.comment.dto.CommentRequest;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.common.error.TooManyRequestsException;
import com.taskflow.label.LabelColor;
import com.taskflow.label.LabelService;
import com.taskflow.label.dto.LabelRequest;
import com.taskflow.security.SecureTokens;
import com.taskflow.user.User;
import com.taskflow.user.UserRepository;
import com.taskflow.workspace.WorkspaceMember;
import com.taskflow.workspace.WorkspaceMemberRepository;
import com.taskflow.workspace.WorkspaceRepository;
import com.taskflow.workspace.WorkspaceRole;
import com.taskflow.workspace.WorkspaceService;
import com.taskflow.workspace.dto.WorkspaceRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Try the demo": every visitor gets a private, pre-filled sandbox (their own account, a
 * teammate, a workspace and a board with cards), so nobody sees or breaks anyone else's demo.
 * Sandboxes expire after {@code app.demo.ttl} and are deleted by {@link DemoCleanup}.
 *
 * <p>The content is created through the same services the API uses, so it always follows the
 * real rules (positions, labels of the board, members as assignees).
 */
@Service
public class DemoService {

    static final String DEMO_EMAIL_DOMAIN = "demo.taskflow.dev";

    private final DemoProperties properties;
    private final DemoRateLimiter rateLimiter;
    private final DemoCleanup cleanup;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceService workspaceService;
    private final BoardService boardService;
    private final LabelService labelService;
    private final CardService cardService;
    private final CommentService commentService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DemoService(
            DemoProperties properties,
            DemoRateLimiter rateLimiter,
            DemoCleanup cleanup,
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository memberRepository,
            WorkspaceService workspaceService,
            BoardService boardService,
            LabelService labelService,
            CardService cardService,
            CommentService commentService,
            PasswordEncoder passwordEncoder,
            Clock clock) {
        this.properties = properties;
        this.rateLimiter = rateLimiter;
        this.cleanup = cleanup;
        this.userRepository = userRepository;
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.workspaceService = workspaceService;
        this.boardService = boardService;
        this.labelService = labelService;
        this.cardService = cardService;
        this.commentService = commentService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** Creates a sandbox and returns its visitor account, ready to be signed in. */
    @Transactional
    public User createSandbox(String clientAddress) {
        if (!properties.enabled()) {
            throw new ResourceNotFoundException("The demo is not available");
        }
        if (!rateLimiter.tryAcquire(clientAddress)) {
            throw new TooManyRequestsException("Too many demos from this address. Please try again in an hour.");
        }
        cleanup.deleteExpired();
        if (userRepository.countByDemoExpiresAtAfter(clock.instant()) >= properties.maxActive()) {
            throw new TooManyRequestsException("The demo is very busy right now. Please try again later.");
        }

        Instant expiresAt = clock.instant().plus(properties.ttl());
        User visitor = demoUser("Demo Visitor", expiresAt);
        User teammate = demoUser("Sam Rivera", expiresAt);
        seed(visitor, teammate);
        return visitor;
    }

    private User demoUser(String displayName, Instant expiresAt) {
        // Nobody knows this password: the account can only be entered through the demo session.
        String email = "demo-" + UUID.randomUUID() + "@" + DEMO_EMAIL_DOMAIN;
        return userRepository.save(User.demo(email, passwordEncoder.encode(SecureTokens.generate()), displayName, expiresAt));
    }

    private void seed(User visitor, User teammate) {
        UUID me = visitor.getId();
        UUID workspaceId = workspaceService.create(me, new WorkspaceRequest("Demo workspace")).id();
        memberRepository.save(new WorkspaceMember(
                workspaceRepository.getReferenceById(workspaceId), teammate, WorkspaceRole.ADMIN));

        UUID boardId = boardService.create(workspaceId, me, new CreateBoardRequest(
                        "Product launch",
                        "A sample board. Drag cards between columns, open one to edit it, or try the filters.",
                        true))
                .id();
        List<UUID> columns = boardService.get(boardId, me, CardFilter.none()).columns().stream()
                .map(BoardDetailResponse.ColumnWithCards::id)
                .toList();
        UUID todo = columns.get(0);
        UUID doing = columns.get(1);
        UUID done = columns.get(2);

        UUID bug = labelService.create(boardId, me, new LabelRequest("Bug", LabelColor.RED)).id();
        UUID design = labelService.create(boardId, me, new LabelRequest("Design", LabelColor.PURPLE)).id();
        UUID docs = labelService.create(boardId, me, new LabelRequest("Docs", LabelColor.BLUE)).id();
        UUID urgent = labelService.create(boardId, me, new LabelRequest("Urgent", LabelColor.ORANGE)).id();

        LocalDate today = LocalDate.now(clock);
        UUID hero = card(me, todo, "Design the landing page hero",
                "Two variants: one with a product screenshot, one with an illustration.",
                teammate.getId(), today.plusDays(2), List.of(design));
        card(me, todo, "Fix the broken links in the footer", null, me, today.minusDays(1), List.of(bug, urgent));
        card(me, todo, "Write the pricing page copy", null, null, today.plusDays(9), List.of(docs));
        card(me, doing, "Set up product analytics", "Track sign-ups and the first board created.",
                me, today.plusDays(5), List.of());
        card(me, doing, "Prepare the launch email", null, teammate.getId(), null, List.of(docs));
        card(me, done, "Choose the brand colours", null, teammate.getId(), null, List.of(design));

        commentService.create(hero, teammate.getId(), new CommentRequest("First drafts are ready. Which one do you prefer?"));
        commentService.create(hero, me, new CommentRequest("Variant A loads faster on mobile. Let's go with that one."));
    }

    private UUID card(
            UUID userId, UUID columnId, String title, String description, UUID assigneeId, LocalDate due, List<UUID> labels) {
        CardResponse created = cardService.create(columnId, userId, new CreateCardRequest(title, null, null, null));
        cardService.update(created.id(), userId,
                new UpdateCardRequest(title, description, assigneeId, due, labels, created.version()));
        return created.id();
    }
}
