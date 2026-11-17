package com.taskflow.card;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.board.BoardTestSupport;
import com.taskflow.common.error.GlobalExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

/** Optimistic locking of cards, through the API (stale versions) and below it (a real race). */
class CardConcurrencyIntegrationTest extends BoardTestSupport {

    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private Team team;
    private TestUser member;
    private UUID boardId;
    private UUID column;
    private UUID cardId;

    @BeforeEach
    void createCard() throws Exception {
        team = createTeam();
        member = team.member();
        boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        column = createColumn(team.owner(), boardId, "To do");
        cardId = createCard(member, column, "Original");
    }

    private JsonNode card() throws Exception {
        return body(perform(member, get("/api/v1/cards/{id}", cardId)).andExpect(status().isOk()));
    }

    private ResultActions edit(String title, long version) throws Exception {
        return perform(member, patch("/api/v1/cards/{id}", cardId), fields("title", title, "version", version));
    }

    @Test
    void editWithTheCurrentVersionSucceedsAndIncreasesTheVersion() throws Exception {
        long version = card().get("version").asLong();

        JsonNode edited = body(edit("Edited", version).andExpect(status().isOk()));

        assertThat(edited.get("title").asText()).isEqualTo("Edited");
        assertThat(edited.get("version").asLong()).isGreaterThan(version);
        assertThat(card().get("version").asLong()).isEqualTo(edited.get("version").asLong());
    }

    @Test
    void editWithAStaleVersionIsRejectedAndChangesNothing() throws Exception {
        long staleVersion = card().get("version").asLong();
        // Somebody else saves first.
        edit("Saved first", staleVersion).andExpect(status().isOk());
        JsonNode before = card();

        edit("Saved late", staleVersion)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString("modified by someone else")));

        JsonNode after = card();
        assertThat(after.get("title").asText()).isEqualTo("Saved first");
        assertThat(after.get("version").asLong()).isEqualTo(before.get("version").asLong());
        assertThat(after.get("updatedAt").asText()).isEqualTo(before.get("updatedAt").asText());
    }

    @Test
    void aVersionFromTheFutureIsRejectedToo() throws Exception {
        long version = card().get("version").asLong();

        edit("Nope", version + 5).andExpect(status().isConflict());

        assertThat(card().get("title").asText()).isEqualTo("Original");
    }

    @Test
    void movingACardDoesNotInvalidateTheCopyAnotherUserHasOpen() throws Exception {
        UUID other = createCard(member, column, "Other");
        JsonNode opened = card();
        long openedVersion = opened.get("version").asLong();

        JsonNode moved = body(moveCard(member, cardId, column, other, null).andExpect(status().isOk()));

        // The move response carries the real version, and it did not change.
        assertThat(moved.get("version").asLong()).isEqualTo(openedVersion);
        assertThat(moved.get("position").asDouble()).isGreaterThan(opened.get("position").asDouble());

        // Saving the copy opened before the move works, and does not undo the move.
        JsonNode edited = body(edit("Edited after move", openedVersion).andExpect(status().isOk()));
        assertThat(edited.get("version").asLong()).isGreaterThan(openedVersion);
        assertThat(edited.get("position").asDouble()).isEqualTo(moved.get("position").asDouble());
        assertThat(cardTitles(member, boardId, column)).containsExactly("Other", "Edited after move");
    }

    /**
     * Two transactions read the same version and both write. The barrier guarantees that both
     * have read before either writes, so no timing is involved: the first UPDATE locks the row,
     * the second one waits for the commit and then matches no row ("version = ?" is stale).
     */
    @Test
    void ofTwoTransactionsThatReadTheSameVersionExactlyOneWins() throws Exception {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        CyclicBarrier bothHaveRead = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (String title : List.of("Writer one", "Writer two")) {
                Callable<Boolean> writer = () -> template.execute(status -> {
                    Card card = cardRepository.findById(cardId).orElseThrow();
                    awaitPeer(bothHaveRead);
                    card.setTitle(title);
                    cardRepository.saveAndFlush(card);
                    return true;
                });
                results.add(executor.submit(writer));
            }

            int winners = 0;
            int optimisticFailures = 0;
            for (Future<Boolean> result : results) {
                try {
                    if (result.get(30, TimeUnit.SECONDS)) {
                        winners++;
                    }
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ObjectOptimisticLockingFailureException.class);
                    optimisticFailures++;
                }
            }

            assertThat(winners).isEqualTo(1);
            assertThat(optimisticFailures).isEqualTo(1);
            // The loser rolled back: the stored title belongs to the winner and the version went
            // up exactly once.
            Card stored = cardRepository.findById(cardId).orElseThrow();
            assertThat(stored.getTitle()).startsWith("Writer ");
            assertThat(stored.getVersion()).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    /** The loser of a race that passed the service check must get 409, not fall through to 500. */
    @Test
    void optimisticLockFailureIsMappedToConflict() {
        ProblemDetail problem = new GlobalExceptionHandler()
                .handleOptimisticLockingFailure(new ObjectOptimisticLockingFailureException(Card.class, cardId));

        assertThat(problem.getStatus()).isEqualTo(409);
        assertThat(problem.getDetail()).contains("modified by someone else");
    }

    private static void awaitPeer(CyclicBarrier barrier) {
        try {
            barrier.await(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("The other transaction did not reach the barrier", e);
        }
    }
}
