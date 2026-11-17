package com.taskflow.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BoardIntegrationTest extends BoardTestSupport {

    @Test
    void adminsAndOwnersCreateBoardsButMembersCannot() throws Exception {
        Team team = createTeam();

        perform(
                        team.owner(),
                        post("/api/v1/workspaces/{id}/boards", team.workspaceId()),
                        Map.of("name", "  Roadmap  ", "description", "   "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.workspaceId").value(team.workspaceId().toString()))
                .andExpect(jsonPath("$.name").value("Roadmap"))
                // A blank description is stored as "no description".
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());

        perform(
                        team.admin(),
                        post("/api/v1/workspaces/{id}/boards", team.workspaceId()),
                        Map.of("name", "Sprint", "description", "Two-week sprint"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Two-week sprint"));

        perform(team.member(), post("/api/v1/workspaces/{id}/boards", team.workspaceId()), Map.of("name", "Nope"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("This action requires the ADMIN role"));
    }

    @Test
    void everyMemberSeesTheBoardsOfTheWorkspaceInCreationOrder() throws Exception {
        Team team = createTeam();
        createBoard(team.owner(), team.workspaceId(), "First");
        createBoard(team.admin(), team.workspaceId(), "Second");
        // A board of another workspace must not show up.
        createBoard(team.member(), createWorkspace(team.member(), "Side project"), "Elsewhere");

        perform(team.member(), get("/api/v1/workspaces/{id}/boards", team.workspaceId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("First", "Second")));
    }

    @Test
    void nameIsRequiredAndFieldsHaveMaximumLengths() throws Exception {
        Team team = createTeam();

        perform(team.owner(), post("/api/v1/workspaces/{id}/boards", team.workspaceId()), Map.of("name", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
        perform(
                        team.owner(),
                        post("/api/v1/workspaces/{id}/boards", team.workspaceId()),
                        Map.of("name", "Ok", "description", "x".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.description").isNotEmpty());
    }

    @Test
    void boardComesWithItsColumnsAndCardsInOrderAndTheCallersRole() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID todo = createColumn(team.owner(), boardId, "To do");
        UUID done = createColumn(team.owner(), boardId, "Done");
        createCard(team.member(), todo, "Write tests");
        createCard(team.member(), todo, "Fix bug");
        createCard(team.member(), done, "Set up CI");

        perform(team.member(), get("/api/v1/boards/{id}", boardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Roadmap"))
                .andExpect(jsonPath("$.workspaceId").value(team.workspaceId().toString()))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.columns[*].name").value(contains("To do", "Done")))
                .andExpect(jsonPath("$.columns[0].cards[*].title").value(contains("Write tests", "Fix bug")))
                .andExpect(jsonPath("$.columns[1].cards[*].title").value(contains("Set up CI")))
                .andExpect(jsonPath("$.columns[0].cards[0].columnId").value(todo.toString()))
                // The board carries summaries: no description.
                .andExpect(jsonPath("$.columns[0].cards[0].description").doesNotExist());

        perform(team.owner(), get("/api/v1/boards/{id}", boardId)).andExpect(jsonPath("$.role").value("OWNER"));
    }

    @Test
    void newBoardStartsWithThreeColumnsUnlessAskedOtherwise() throws Exception {
        Team team = createTeam();

        UUID withDefaults = idOf(perform(
                        team.owner(), post("/api/v1/workspaces/{id}/boards", team.workspaceId()), Map.of("name", "Roadmap"))
                .andExpect(status().isCreated()));
        assertThat(columnNames(team.member(), withDefaults)).containsExactly("To do", "In progress", "Done");

        UUID explicit = idOf(perform(
                        team.owner(),
                        post("/api/v1/workspaces/{id}/boards", team.workspaceId()),
                        Map.of("name", "Sprint", "defaultColumns", true))
                .andExpect(status().isCreated()));
        assertThat(columnNames(team.member(), explicit)).hasSize(3);

        // Columns added later go after the default ones.
        createColumn(team.owner(), withDefaults, "Archived");
        assertThat(columnNames(team.member(), withDefaults)).containsExactly("To do", "In progress", "Done", "Archived");
    }

    @Test
    void emptyBoardHasNoColumns() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Empty");

        perform(team.member(), get("/api/v1/boards/{id}", boardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.columns.length()").value(0));
    }

    @Test
    void outsidersCannotTellAnExistingBoardFromAMissingOne() throws Exception {
        Team team = createTeam();
        TestUser mallory = registerUser("Mallory");
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Private");

        perform(mallory, get("/api/v1/workspaces/{id}/boards", team.workspaceId())).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/workspaces/{id}/boards", team.workspaceId()), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound());
        perform(mallory, patch("/api/v1/boards/{id}", boardId), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/boards/{id}", boardId)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/boards/{id}/columns", boardId), Map.of("name", "Hacked"))
                .andExpect(status().isNotFound());

        // Byte-for-byte the same body for a real board and for an id that does not exist.
        String forExisting = perform(mallory, get("/api/v1/boards/{id}", boardId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Board not found"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        UUID missing = UUID.randomUUID();
        String forMissing = perform(mallory, get("/api/v1/boards/{id}", missing))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(forExisting.replace(boardId.toString(), "ID")).isEqualTo(forMissing.replace(missing.toString(), "ID"));
    }

    @Test
    void adminsUpdateBoardsButMembersCannot() throws Exception {
        Team team = createTeam();
        UUID boardId = idOf(perform(
                        team.owner(),
                        post("/api/v1/workspaces/{id}/boards", team.workspaceId()),
                        Map.of("name", "Roadmap", "description", "Old"))
                .andExpect(status().isCreated()));

        perform(team.member(), patch("/api/v1/boards/{id}", boardId), Map.of("name", "Nope"))
                .andExpect(status().isForbidden());

        // The description is not sent, so it is cleared.
        perform(team.admin(), patch("/api/v1/boards/{id}", boardId), Map.of("name", "Roadmap 2027"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Roadmap 2027"))
                .andExpect(jsonPath("$.description").isEmpty());
        perform(team.member(), get("/api/v1/boards/{id}", boardId))
                .andExpect(jsonPath("$.name").value("Roadmap 2027"));
    }

    @Test
    void deletingABoardRemovesItsColumnsAndCards() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Short-lived");
        UUID columnId = createColumn(team.owner(), boardId, "To do");
        UUID cardId = createCard(team.member(), columnId, "Task");

        perform(team.member(), delete("/api/v1/boards/{id}", boardId)).andExpect(status().isForbidden());
        perform(team.admin(), delete("/api/v1/boards/{id}", boardId)).andExpect(status().isNoContent());

        perform(team.owner(), get("/api/v1/boards/{id}", boardId)).andExpect(status().isNotFound());
        perform(team.owner(), get("/api/v1/cards/{id}", cardId)).andExpect(status().isNotFound());
        perform(team.owner(), get("/api/v1/workspaces/{id}/boards", team.workspaceId()))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deletingAWorkspaceRemovesItsBoards() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID cardId = createCard(team.owner(), createColumn(team.owner(), boardId, "To do"), "Task");

        perform(team.owner(), delete("/api/v1/workspaces/{id}", team.workspaceId())).andExpect(status().isNoContent());

        perform(team.owner(), get("/api/v1/boards/{id}", boardId)).andExpect(status().isNotFound());
        perform(team.owner(), get("/api/v1/cards/{id}", cardId)).andExpect(status().isNotFound());
    }
}
