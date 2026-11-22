package com.taskflow.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The board view with filters. Fixture, all on one board:
 * <ul>
 *   <li>To do: "Fix login bug" (member, Bug, overdue), "Write docs" (admin, due in 3 days)</li>
 *   <li>Done: "Release 100% coverage" (unassigned, Bug, no date), "Plan sprint_2" (member, due in 30 days)</li>
 * </ul>
 */
class BoardFilterIntegrationTest extends BoardTestSupport {

    private Team team;
    private UUID boardId;
    private UUID bug;
    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);

    @BeforeEach
    void createBoardWithCards() throws Exception {
        team = createTeam();
        boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID todo = createColumn(team.owner(), boardId, "To do");
        UUID done = createColumn(team.owner(), boardId, "Done");
        bug = createLabel(team.owner(), boardId, "Bug", "RED");

        card(todo, "Fix login bug", "Users cannot sign in", team.member().id(), today.minusDays(1), bug);
        card(todo, "Write docs", "Explain the LOGIN flow", team.admin().id(), today.plusDays(3), null);
        card(done, "Release 100% coverage", null, null, null, bug);
        card(done, "Plan sprint_2", null, team.member().id(), today.plusDays(30), null);
    }

    private void card(UUID columnId, String title, String description, UUID assigneeId, LocalDate due, UUID labelId)
            throws Exception {
        UUID cardId = createCard(team.member(), columnId, title);
        editCard(team.member(), cardId, fields(
                        "title", title,
                        "description", description,
                        "assigneeId", assigneeId,
                        "dueDate", due == null ? null : due.toString(),
                        "labelIds", labelId == null ? List.of() : List.of(labelId)))
                .andExpect(status().isOk());
    }

    /** Titles of the matching cards, column by column in board order. */
    private List<String> titles(String... params) throws Exception {
        var request = get("/api/v1/boards/{id}", boardId);
        for (int i = 0; i < params.length; i += 2) {
            request.param(params[i], params[i + 1]);
        }
        JsonNode board = body(perform(team.member(), request).andExpect(status().isOk()));
        List<String> titles = new ArrayList<>();
        board.get("columns").forEach(column -> column.get("cards").forEach(card -> titles.add(card.get("title").asText())));
        return titles;
    }

    @Test
    void withoutFiltersEveryCardIsReturned() throws Exception {
        assertThat(titles()).containsExactly("Fix login bug", "Write docs", "Release 100% coverage", "Plan sprint_2");
    }

    @Test
    void textSearchIgnoresCaseAndLooksInTheDescription() throws Exception {
        assertThat(titles("q", "LoGiN")).containsExactly("Fix login bug", "Write docs");
        assertThat(titles("q", "  sign in ")).containsExactly("Fix login bug");
        assertThat(titles("q", "nothing like this")).isEmpty();
    }

    @Test
    void wildcardCharactersAreSearchedLiterally() throws Exception {
        // Unescaped, "%" would match everything and "_" any single character.
        assertThat(titles("q", "%")).containsExactly("Release 100% coverage");
        assertThat(titles("q", "_")).containsExactly("Plan sprint_2");
        assertThat(titles("q", "\\")).isEmpty();
    }

    @Test
    void filtersByAssigneeOrUnassigned() throws Exception {
        assertThat(titles("assigneeId", team.member().id().toString())).containsExactly("Fix login bug", "Plan sprint_2");
        assertThat(titles("unassigned", "true")).containsExactly("Release 100% coverage");
    }

    @Test
    void filtersByLabel() throws Exception {
        assertThat(titles("labelId", bug.toString())).containsExactly("Fix login bug", "Release 100% coverage");
    }

    @Test
    void filtersByDueDate() throws Exception {
        assertThat(titles("due", "OVERDUE")).containsExactly("Fix login bug");
        assertThat(titles("due", "WEEK")).containsExactly("Write docs");
        assertThat(titles("due", "NONE")).containsExactly("Release 100% coverage");
    }

    @Test
    void filtersAreCombinedWithAnd() throws Exception {
        assertThat(titles("q", "login", "labelId", bug.toString())).containsExactly("Fix login bug");
        assertThat(titles("labelId", bug.toString(), "unassigned", "true")).containsExactly("Release 100% coverage");
        assertThat(titles("q", "docs", "due", "OVERDUE")).isEmpty();
    }

    @Test
    void everyColumnIsReturnedEvenWhenNoCardMatches() throws Exception {
        perform(team.member(), get("/api/v1/boards/{id}", boardId).param("q", "zzz"))
                .andExpect(jsonPath("$.columns.length()").value(2))
                .andExpect(jsonPath("$.columns[0].cards.length()").value(0));
    }

    @Test
    void invalidFilterValuesAreBadRequests() throws Exception {
        perform(team.member(), get("/api/v1/boards/{id}", boardId).param("due", "tomorrow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.due").isNotEmpty());
        perform(team.member(), get("/api/v1/boards/{id}", boardId).param("labelId", "not-a-uuid"))
                .andExpect(status().isBadRequest());
        perform(team.member(), get("/api/v1/boards/{id}", boardId).param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.q").isNotEmpty());
    }

    @Test
    void filtersStillRespectWorkspaceAccess() throws Exception {
        perform(registerUser("Mallory"), get("/api/v1/boards/{id}", boardId).param("q", "login"))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCardCanBeMovedBetweenTwoVisibleCardsWhileFiltered() throws Exception {
        // Unfiltered "To do" order: Fix login bug, Write docs. With ?q=login both are visible;
        // dropping a new card between them must land it between them for everyone.
        JsonNode filtered = body(perform(team.member(), get("/api/v1/boards/{id}", boardId).param("q", "login")));
        UUID todo = UUID.fromString(filtered.at("/columns/0/id").asText());
        UUID first = UUID.fromString(filtered.at("/columns/0/cards/0/id").asText());
        UUID second = UUID.fromString(filtered.at("/columns/0/cards/1/id").asText());
        UUID moved = createCard(team.member(), todo, "Moved");

        moveCard(team.member(), moved, todo, first, second).andExpect(status().isOk());

        assertThat(cardTitles(team.member(), boardId, todo)).containsExactly("Fix login bug", "Moved", "Write docs");
    }
}
