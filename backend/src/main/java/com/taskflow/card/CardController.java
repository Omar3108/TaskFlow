package com.taskflow.card;

import com.taskflow.card.dto.CardResponse;
import com.taskflow.card.dto.CreateCardRequest;
import com.taskflow.card.dto.MoveCardRequest;
import com.taskflow.card.dto.UpdateCardRequest;
import com.taskflow.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PostMapping("/columns/{columnId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a card at the bottom of a column")
    public CardResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID columnId,
            @Valid @RequestBody CreateCardRequest request) {
        return cardService.create(columnId, user.id(), request);
    }

    @GetMapping("/cards/{cardId}")
    @Operation(summary = "Get a card with its description")
    public CardResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID cardId) {
        return cardService.get(cardId, user.id());
    }

    @PatchMapping("/cards/{cardId}")
    @Operation(summary = "Edit a card; a stale version is rejected with 409")
    public CardResponse update(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody UpdateCardRequest request) {
        return cardService.update(cardId, user.id(), request);
    }

    // A dedicated endpoint instead of PATCH /cards/{id} with a position: the client describes
    // the drop (target column + neighbours) and the server owns the ordering rules.
    @PatchMapping("/cards/{cardId}/move")
    @Operation(summary = "Move a card within its column or to another column of the same board")
    public CardResponse move(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID cardId,
            @Valid @RequestBody MoveCardRequest request) {
        return cardService.move(cardId, user.id(), request);
    }

    @DeleteMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a card")
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID cardId) {
        cardService.delete(cardId, user.id());
    }
}
