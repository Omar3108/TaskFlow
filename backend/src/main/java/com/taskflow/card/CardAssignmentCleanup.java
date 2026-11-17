package com.taskflow.card;

import com.taskflow.workspace.MemberRemovedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Keeps the rule "the assignee of a card is a member of the workspace" true when members leave. */
@Component
public class CardAssignmentCleanup {

    private final CardRepository cardRepository;

    public CardAssignmentCleanup(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    // A plain @EventListener runs synchronously in the thread of the publisher, and therefore
    // inside the transaction that removes the member.
    @EventListener
    public void onMemberRemoved(MemberRemovedEvent event) {
        cardRepository.unassignAllInWorkspace(event.workspaceId(), event.userId());
    }
}
