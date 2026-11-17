package com.taskflow.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ColumnIntegrationTest extends BoardTestSupport {

    @Test
    void newColumnsGoToTheEndOfTheBoard() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");

        JsonNode first = body(perform(team.admin(), post("/api/v1/boards/{id}/columns", boardId), Map.of("name", " To do "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.boardId").value(boardId.toString()))
                .andExpect(jsonPath("$.name").value("To do")));
        JsonNode second = body(perform(team.owner(), post("/api/v1/boards/{id}/columns", boardId), Map.of("name", "Doing"))
                .andExpect(status().isCreated()));
        createColumn(team.owner(), boardId, "Done");

        assertThat(second.get("position").asDouble()).isGreaterThan(first.get("position").asDouble());
        assertThat(columnNames(team.member(), boardId)).containsExactly("To do", "Doing", "Done");
    }

    @Test
    void membersCannotManageColumns() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID columnId = createColumn(team.owner(), boardId, "To do");

        perform(team.member(), post("/api/v1/boards/{id}/columns", boardId), Map.of("name", "Nope"))
                .andExpect(status().isForbidden());
        perform(team.member(), patch("/api/v1/columns/{id}", columnId), Map.of("name", "Nope"))
                .andExpect(status().isForbidden());
        moveColumn(team.member(), columnId, null, null).andExpect(status().isForbidden());
        perform(team.member(), delete("/api/v1/columns/{id}", columnId)).andExpect(status().isForbidden());

        assertThat(columnNames(team.member(), boardId)).containsExactly("To do");
    }

    @Test
    void adminsRenameColumns() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID columnId = createColumn(team.owner(), boardId, "To do");

        perform(team.admin(), patch("/api/v1/columns/{id}", columnId), Map.of("name", "Backlog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Backlog"));
        perform(team.admin(), patch("/api/v1/columns/{id}", columnId), Map.of("name", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());

        assertThat(columnNames(team.member(), boardId)).containsExactly("Backlog");
    }

    @Test
    void deletingAColumnRemovesItsCards() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID todo = createColumn(team.owner(), boardId, "To do");
        UUID done = createColumn(team.owner(), boardId, "Done");
        UUID doomed = createCard(team.member(), todo, "Doomed");
        UUID survivor = createCard(team.member(), done, "Survivor");

        perform(team.admin(), delete("/api/v1/columns/{id}", todo)).andExpect(status().isNoContent());

        assertThat(columnNames(team.member(), boardId)).containsExactly("Done");
        perform(team.member(), get("/api/v1/cards/{id}", doomed)).andExpect(status().isNotFound());
        perform(team.member(), get("/api/v1/cards/{id}", survivor)).andExpect(status().isOk());
    }

    @Test
    void outsidersGetNotFound() throws Exception {
        Team team = createTeam();
        TestUser mallory = registerUser("Mallory");
        UUID columnId = createColumn(team.owner(), createBoard(team.owner(), team.workspaceId(), "Private"), "To do");

        perform(mallory, patch("/api/v1/columns/{id}", columnId), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Column not found"));
        moveColumn(mallory, columnId, null, null).andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/columns/{id}", columnId)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/columns/{id}/cards", columnId), Map.of("title", "Hacked"))
                .andExpect(status().isNotFound());

        // Same answer as for an id that really does not exist.
        perform(mallory, patch("/api/v1/columns/{id}", UUID.randomUUID()), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Column not found"));
    }

    @Test
    void moveRejectsNeighboursThatDoNotFit() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID first = createColumn(team.owner(), boardId, "First");
        UUID second = createColumn(team.owner(), boardId, "Second");
        UUID third = createColumn(team.owner(), boardId, "Third");
        UUID foreign = createColumn(team.owner(), createBoard(team.owner(), team.workspaceId(), "Other"), "Foreign");

        // A neighbour from another board, even one the caller can see.
        moveColumn(team.admin(), first, foreign, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("The neighbour columns must belong to the same board"));
        moveColumn(team.admin(), first, null, UUID.randomUUID()).andExpect(status().isBadRequest());
        // The column itself as its own neighbour.
        moveColumn(team.admin(), first, first, second).andExpect(status().isBadRequest());
        // Neighbours in the wrong order.
        moveColumn(team.admin(), first, third, second).andExpect(status().isBadRequest());

        assertThat(columnNames(team.member(), boardId)).containsExactly("First", "Second", "Third");
    }

    /** Enabled in phase 3b, once PositionCalculator.between exists. */
    @Nested
    @Disabled("Pending PositionCalculator.between (phase 3b)")
    class Move {

        @Test
        void toTheStart() throws Exception {
            Team team = createTeam();
            UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
            UUID first = createColumn(team.owner(), boardId, "First");
            createColumn(team.owner(), boardId, "Second");
            UUID third = createColumn(team.owner(), boardId, "Third");

            moveColumn(team.admin(), third, null, first)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(third.toString()));

            assertThat(columnNames(team.member(), boardId)).containsExactly("Third", "First", "Second");
        }

        @Test
        void betweenTwoColumns() throws Exception {
            Team team = createTeam();
            UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
            UUID first = createColumn(team.owner(), boardId, "First");
            UUID second = createColumn(team.owner(), boardId, "Second");
            UUID third = createColumn(team.owner(), boardId, "Third");

            moveColumn(team.admin(), third, first, second).andExpect(status().isOk());

            assertThat(columnNames(team.member(), boardId)).containsExactly("First", "Third", "Second");
        }

        @Test
        void toTheEnd() throws Exception {
            Team team = createTeam();
            UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
            UUID first = createColumn(team.owner(), boardId, "First");
            createColumn(team.owner(), boardId, "Second");
            UUID third = createColumn(team.owner(), boardId, "Third");

            moveColumn(team.admin(), first, third, null).andExpect(status().isOk());

            assertThat(columnNames(team.member(), boardId)).containsExactly("Second", "Third", "First");
        }

        @Test
        void movingDoesNotTouchTheCards() throws Exception {
            Team team = createTeam();
            UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
            UUID first = createColumn(team.owner(), boardId, "First");
            UUID second = createColumn(team.owner(), boardId, "Second");
            createCard(team.member(), first, "A");
            createCard(team.member(), first, "B");

            moveColumn(team.admin(), first, second, null).andExpect(status().isOk());

            assertThat(cardTitles(team.member(), boardId, first)).containsExactly("A", "B");
        }
    }
}
