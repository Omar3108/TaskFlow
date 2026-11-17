package com.taskflow.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.board.BoardTestSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Specification of {@code PATCH /cards/{id}/move}. Tests only look at the resulting order,
 * never at exact position values, so any correct position strategy passes.
 */
class CardMoveIntegrationTest extends BoardTestSupport {

    private Team team;
    private TestUser member;
    private UUID boardId;
    private UUID todo;
    private UUID done;
    private UUID a;
    private UUID b;
    private UUID c;

    /** To do: A, B, C. Done: empty. */
    @BeforeEach
    void createBoardWithThreeCards() throws Exception {
        team = createTeam();
        member = team.member();
        boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        todo = createColumn(team.owner(), boardId, "To do");
        done = createColumn(team.owner(), boardId, "Done");
        a = createCard(member, todo, "A");
        b = createCard(member, todo, "B");
        c = createCard(member, todo, "C");
    }

    @Test
    void toTheTopOfTheSameColumn() throws Exception {
        moveCard(member, c, todo, null, a)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(c.toString()))
                .andExpect(jsonPath("$.columnId").value(todo.toString()));

        assertThat(cardTitles(member, boardId, todo)).containsExactly("C", "A", "B");
    }

    @Test
    void betweenTwoCardsOfTheSameColumn() throws Exception {
        moveCard(member, c, todo, a, b).andExpect(status().isOk());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("A", "C", "B");
    }

    @Test
    void toTheBottomOfTheSameColumn() throws Exception {
        moveCard(member, a, todo, c, null).andExpect(status().isOk());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("B", "C", "A");
    }

    @Test
    void droppingACardWhereItAlreadyIsChangesNothing() throws Exception {
        moveCard(member, b, todo, a, c).andExpect(status().isOk());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("A", "B", "C");
    }

    @Test
    void toAnEmptyColumn() throws Exception {
        moveCard(member, b, done, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columnId").value(done.toString()));

        assertThat(cardTitles(member, boardId, todo)).containsExactly("A", "C");
        assertThat(cardTitles(member, boardId, done)).containsExactly("B");
    }

    @Test
    void betweenTwoCardsOfAnotherColumn() throws Exception {
        UUID x = createCard(member, done, "X");
        UUID y = createCard(member, done, "Y");

        moveCard(member, a, done, x, y).andExpect(status().isOk());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("B", "C");
        assertThat(cardTitles(member, boardId, done)).containsExactly("X", "A", "Y");
    }

    @Test
    void movingKeepsTheRestOfTheCardData() throws Exception {
        moveCard(member, a, done, null, null).andExpect(status().isOk());

        perform(member, get("/api/v1/cards/{id}", a))
                .andExpect(jsonPath("$.title").value("A"))
                .andExpect(jsonPath("$.columnId").value(done.toString()))
                .andExpect(jsonPath("$.boardId").value(boardId.toString()));
    }

    @Test
    void targetColumnMustBelongToTheSameBoard() throws Exception {
        UUID otherBoardColumn =
                createColumn(team.owner(), createBoard(team.owner(), team.workspaceId(), "Other board"), "To do");

        moveCard(member, a, otherBoardColumn, null, null).andExpect(status().isBadRequest());
        moveCard(member, a, UUID.randomUUID(), null, null).andExpect(status().isBadRequest());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("A", "B", "C");
    }

    @Test
    void neighboursMustBeCardsOfTheTargetColumn() throws Exception {
        UUID x = createCard(member, done, "X");

        // B and C live in "To do", not in the target column.
        moveCard(member, a, done, b, null).andExpect(status().isBadRequest());
        moveCard(member, a, done, x, c).andExpect(status().isBadRequest());
        moveCard(member, a, done, UUID.randomUUID(), null).andExpect(status().isBadRequest());
        // The moved card cannot be its own neighbour.
        moveCard(member, a, todo, a, b).andExpect(status().isBadRequest());
        // Neighbours in the wrong order.
        moveCard(member, a, todo, c, b).andExpect(status().isBadRequest());

        assertThat(cardTitles(member, boardId, todo)).containsExactly("A", "B", "C");
        assertThat(cardTitles(member, boardId, done)).containsExactly("X");
    }

    @Test
    void neighboursFromAnotherWorkspaceAreRejected() throws Exception {
        TestUser stranger = registerUser("Stranger");
        UUID strangersColumn = createColumn(
                stranger, createBoard(stranger, createWorkspace(stranger, "Elsewhere"), "Their board"), "To do");
        UUID strangersCard = createCard(stranger, strangersColumn, "Not yours");

        moveCard(member, a, todo, strangersCard, null).andExpect(status().isBadRequest());
        moveCard(member, a, strangersColumn, null, null).andExpect(status().isBadRequest());
    }

    /**
     * Every iteration drops the last card between the first two, which halves the gap between
     * them. A double runs out of precision after roughly 50 halvings, so this only passes if
     * the column is rebalanced along the way.
     */
    @Test
    void orderSurvivesManyMovesIntoTheSameGap() throws Exception {
        Map<UUID, String> titles = new LinkedHashMap<>();
        titles.put(a, "A");
        titles.put(b, "B");
        titles.put(c, "C");
        List<UUID> order = new ArrayList<>(List.of(a, b, c));

        for (int i = 0; i < 80; i++) {
            UUID moved = order.remove(2);
            moveCard(member, moved, todo, order.get(0), order.get(1)).andExpect(status().isOk());
            order.add(1, moved);

            if (i % 10 == 9) {
                assertThat(cardTitles(member, boardId, todo))
                        .as("after %d moves", i + 1)
                        .containsExactlyElementsOf(order.stream().map(titles::get).toList());
            }
        }
    }
}
