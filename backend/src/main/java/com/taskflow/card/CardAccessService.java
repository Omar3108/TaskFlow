package com.taskflow.card;

import com.taskflow.board.BoardAccessService;
import com.taskflow.common.error.ResourceNotFoundException;
import com.taskflow.workspace.WorkspaceAccessService;
import com.taskflow.workspace.WorkspaceMember;
import com.taskflow.workspace.WorkspaceRole;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a card and checks that the caller is a member of its workspace. Shared by the card and
 * comment features, so both follow the same card → column → board → workspace chain.
 */
@Service
public class CardAccessService {

    public static final String CARD_NOT_FOUND = "Card not found";

    private final CardRepository cardRepository;
    private final WorkspaceAccessService workspaceAccess;

    public CardAccessService(CardRepository cardRepository, WorkspaceAccessService workspaceAccess) {
        this.cardRepository = cardRepository;
        this.workspaceAccess = workspaceAccess;
    }

    /** A card the caller can use, plus the caller's membership (comments need the role). */
    public record CardAccess(Card card, WorkspaceMember member) {
    }

    /** Every card operation is open to plain members, so there is no minimum-role parameter. */
    @Transactional(readOnly = true)
    public CardAccess requireCard(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdWithBoard(cardId)
                .orElseThrow(() -> new ResourceNotFoundException(CARD_NOT_FOUND));
        WorkspaceMember member = workspaceAccess.requireRole(
                BoardAccessService.workspaceIdOf(card.getColumn()), userId, WorkspaceRole.MEMBER, CARD_NOT_FOUND);
        return new CardAccess(card, member);
    }
}
