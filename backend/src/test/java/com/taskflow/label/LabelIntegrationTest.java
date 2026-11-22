package com.taskflow.label;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.board.BoardTestSupport;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LabelIntegrationTest extends BoardTestSupport {

    @Test
    void adminsManageLabelsAndEveryMemberSeesThemAlphabetically() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");

        perform(team.admin(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", " Urgent ", "color", "RED"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("Urgent"))
                .andExpect(jsonPath("$.color").value("RED"));
        createLabel(team.owner(), boardId, "bug", "ORANGE");

        perform(team.member(), get("/api/v1/boards/{id}/labels", boardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name").value(contains("bug", "Urgent")));
    }

    @Test
    void membersCannotManageLabels() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID labelId = createLabel(team.owner(), boardId, "Bug", "RED");

        perform(team.member(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", "Nope", "color", "RED"))
                .andExpect(status().isForbidden());
        perform(team.member(), patch("/api/v1/labels/{id}", labelId), Map.of("name", "Nope", "color", "RED"))
                .andExpect(status().isForbidden());
        perform(team.member(), delete("/api/v1/labels/{id}", labelId)).andExpect(status().isForbidden());
    }

    @Test
    void namesAreUniquePerBoardIgnoringCase() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID bug = createLabel(team.owner(), boardId, "Bug", "RED");
        UUID feature = createLabel(team.owner(), boardId, "Feature", "GREEN");

        perform(team.owner(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", "BUG", "color", "BLUE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("This board already has a label with that name"));
        perform(team.owner(), patch("/api/v1/labels/{id}", feature), Map.of("name", "bug", "color", "GREEN"))
                .andExpect(status().isConflict());

        // Renaming a label to itself (another case) is fine, and so is the same name elsewhere.
        perform(team.owner(), patch("/api/v1/labels/{id}", bug), Map.of("name", "BUG", "color", "PURPLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("BUG"))
                .andExpect(jsonPath("$.color").value("PURPLE"));
        createLabel(team.owner(), createBoard(team.owner(), team.workspaceId(), "Other"), "Bug", "RED");
    }

    @Test
    void colorMustComeFromThePaletteAndNameIsRequired() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");

        perform(team.owner(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", "Bug", "color", "#ff0000"))
                .andExpect(status().isBadRequest());
        perform(team.owner(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", " ", "color", "RED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").isNotEmpty());
        perform(team.owner(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", "x".repeat(31), "color", "RED"))
                .andExpect(status().isBadRequest());
        perform(team.owner(), post("/api/v1/boards/{id}/labels", boardId), Map.of("name", "Bug"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.color").isNotEmpty());
    }

    @Test
    void cardsCarryTheirLabelsOnTheBoardAndInTheCard() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID urgent = createLabel(team.owner(), boardId, "Urgent", "RED");
        UUID bug = createLabel(team.owner(), boardId, "Bug", "ORANGE");
        UUID cardId = createCard(team.member(), createColumn(team.owner(), boardId, "To do"), "Crash on login");

        editCard(team.member(), cardId, Map.of("title", "Crash on login", "labelIds", List.of(urgent, bug, bug)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.labels[*].name").value(contains("Bug", "Urgent")))
                .andExpect(jsonPath("$.labels[1].color").value("RED"));

        perform(team.member(), get("/api/v1/boards/{id}", boardId))
                .andExpect(jsonPath("$.columns[0].cards[0].labels[*].name").value(contains("Bug", "Urgent")));

        // Sending fewer labels removes the others; an empty list removes all of them.
        editCard(team.member(), cardId, Map.of("title", "Crash on login", "labelIds", List.of(urgent)))
                .andExpect(jsonPath("$.labels[*].name").value(contains("Urgent")));
        editCard(team.member(), cardId, Map.of("title", "Crash on login"))
                .andExpect(jsonPath("$.labels.length()").value(0));
    }

    @Test
    void changingTheLabelsOfACardBumpsItsVersion() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID bug = createLabel(team.owner(), boardId, "Bug", "RED");
        UUID cardId = createCard(team.member(), createColumn(team.owner(), boardId, "To do"), "Task");
        long before = body(perform(team.member(), get("/api/v1/cards/{id}", cardId))).get("version").asLong();

        long after = body(editCard(team.member(), cardId, Map.of("title", "Task", "labelIds", List.of(bug))))
                .get("version")
                .asLong();

        assertThat(after).isGreaterThan(before);
    }

    @Test
    void aCardOnlyTakesLabelsOfItsOwnBoard() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID otherBoardLabel = createLabel(team.owner(), createBoard(team.owner(), team.workspaceId(), "Other"), "Bug", "RED");
        UUID cardId = createCard(team.member(), createColumn(team.owner(), boardId, "To do"), "Task");

        for (UUID invalid : new UUID[] {otherBoardLabel, UUID.randomUUID()}) {
            editCard(team.member(), cardId, Map.of("title", "Task", "labelIds", List.of(invalid)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Every label must belong to the board of the card"));
        }
    }

    @Test
    void deletingALabelRemovesItFromEveryCard() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        UUID bug = createLabel(team.owner(), boardId, "Bug", "RED");
        UUID keep = createLabel(team.owner(), boardId, "Keep", "BLUE");
        UUID cardId = createCard(team.member(), createColumn(team.owner(), boardId, "To do"), "Task");
        editCard(team.member(), cardId, Map.of("title", "Task", "labelIds", List.of(bug, keep)))
                .andExpect(status().isOk());

        perform(team.admin(), delete("/api/v1/labels/{id}", bug)).andExpect(status().isNoContent());

        perform(team.member(), get("/api/v1/cards/{id}", cardId))
                .andExpect(jsonPath("$.labels[*].name").value(contains("Keep")));
        perform(team.member(), get("/api/v1/boards/{id}/labels", boardId))
                .andExpect(jsonPath("$[*].name").value(contains("Keep")));
    }

    @Test
    void outsidersGetNotFound() throws Exception {
        Team team = createTeam();
        TestUser mallory = registerUser("Mallory");
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Private");
        UUID labelId = createLabel(team.owner(), boardId, "Bug", "RED");

        perform(mallory, get("/api/v1/boards/{id}/labels", boardId)).andExpect(status().isNotFound());
        perform(mallory, patch("/api/v1/labels/{id}", labelId), Map.of("name", "Hacked", "color", "RED"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Label not found"));
        perform(mallory, delete("/api/v1/labels/{id}", labelId)).andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/labels/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Label not found"));
    }
}
