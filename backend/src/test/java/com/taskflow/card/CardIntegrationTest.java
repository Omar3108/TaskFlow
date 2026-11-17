package com.taskflow.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.board.BoardTestSupport;
import com.taskflow.workspace.WorkspaceRole;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CardIntegrationTest extends BoardTestSupport {

    /** A team with one board and one column, which is all most card tests need. */
    private record Fixture(Team team, UUID boardId, UUID columnId) {
    }

    private Fixture fixture() throws Exception {
        Team team = createTeam();
        UUID boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        return new Fixture(team, boardId, createColumn(team.owner(), boardId, "To do"));
    }

    private long versionOf(TestUser user, UUID cardId) throws Exception {
        return body(perform(user, get("/api/v1/cards/{id}", cardId)).andExpect(status().isOk()))
                .get("version")
                .asLong();
    }

    @Test
    void anyMemberCreatesCards() throws Exception {
        Fixture f = fixture();

        perform(f.team().member(), post("/api/v1/columns/{id}/cards", f.columnId()), Map.of("title", "  Write tests  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.boardId").value(f.boardId().toString()))
                .andExpect(jsonPath("$.columnId").value(f.columnId().toString()))
                .andExpect(jsonPath("$.title").value("Write tests"))
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.dueDate").isEmpty())
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void newCardsGoToTheBottomOfTheColumn() throws Exception {
        Fixture f = fixture();
        createCard(f.team().member(), f.columnId(), "First");
        createCard(f.team().admin(), f.columnId(), "Second");
        createCard(f.team().owner(), f.columnId(), "Third");

        assertThat(cardTitles(f.team().member(), f.boardId(), f.columnId())).containsExactly("First", "Second", "Third");
    }

    @Test
    void cardCanBeCreatedWithEveryField() throws Exception {
        Fixture f = fixture();
        TestUser admin = f.team().admin();

        JsonNode created = body(perform(
                        f.team().member(),
                        post("/api/v1/columns/{id}/cards", f.columnId()),
                        Map.of(
                                "title", "Deploy",
                                "description", "Ship it to production",
                                "assigneeId", admin.id().toString(),
                                "dueDate", "2027-01-31"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Ship it to production"))
                .andExpect(jsonPath("$.assignee.id").value(admin.id().toString()))
                .andExpect(jsonPath("$.assignee.displayName").value("Admin"))
                .andExpect(jsonPath("$.assignee.email").value(admin.email()))
                .andExpect(jsonPath("$.dueDate").value("2027-01-31")));

        // The single-card endpoint returns the same data, and the board shows the assignee too.
        perform(f.team().owner(), get("/api/v1/cards/{id}", created.get("id").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Ship it to production"))
                .andExpect(jsonPath("$.assignee.displayName").value("Admin"));
        perform(f.team().owner(), get("/api/v1/boards/{id}", f.boardId()))
                .andExpect(jsonPath("$.columns[0].cards[0].assignee.displayName").value("Admin"))
                .andExpect(jsonPath("$.columns[0].cards[0].dueDate").value("2027-01-31"));
    }

    @Test
    void titleIsRequiredAndFieldsHaveMaximumLengths() throws Exception {
        Fixture f = fixture();

        perform(f.team().member(), post("/api/v1/columns/{id}/cards", f.columnId()), Map.of("title", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
        perform(f.team().member(), post("/api/v1/columns/{id}/cards", f.columnId()), Map.of("title", "x".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
        perform(
                        f.team().member(),
                        post("/api/v1/columns/{id}/cards", f.columnId()),
                        Map.of("title", "Ok", "dueDate", "next friday"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assigneeMustBeAMemberOfTheWorkspace() throws Exception {
        Fixture f = fixture();
        TestUser outsider = registerUser("Outsider");

        for (UUID notAMember : new UUID[] {outsider.id(), UUID.randomUUID()}) {
            perform(
                            f.team().member(),
                            post("/api/v1/columns/{id}/cards", f.columnId()),
                            Map.of("title", "Task", "assigneeId", notAMember.toString()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("The assignee must be a member of the workspace"));
        }

        UUID cardId = createCard(f.team().member(), f.columnId(), "Task");
        perform(
                        f.team().member(),
                        patch("/api/v1/cards/{id}", cardId),
                        fields("title", "Task", "assigneeId", outsider.id(), "version", versionOf(f.team().member(), cardId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editReplacesEveryEditableField() throws Exception {
        Fixture f = fixture();
        TestUser member = f.team().member();
        UUID cardId = idOf(perform(
                        member,
                        post("/api/v1/columns/{id}/cards", f.columnId()),
                        Map.of(
                                "title", "Draft",
                                "description", "Old text",
                                "assigneeId", member.id().toString(),
                                "dueDate", "2027-01-31"))
                .andExpect(status().isCreated()));

        perform(
                        f.team().admin(),
                        patch("/api/v1/cards/{id}", cardId),
                        fields(
                                "title", " Final ",
                                "description", "New text",
                                "assigneeId", f.team().admin().id(),
                                "dueDate", "2027-02-15",
                                "version", versionOf(member, cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.description").value("New text"))
                .andExpect(jsonPath("$.assignee.displayName").value("Admin"))
                .andExpect(jsonPath("$.dueDate").value("2027-02-15"))
                // An edit never moves the card.
                .andExpect(jsonPath("$.columnId").value(f.columnId().toString()));

        // Fields that are not sent are cleared.
        perform(
                        member,
                        patch("/api/v1/cards/{id}", cardId),
                        fields("title", "Final", "version", versionOf(member, cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").isEmpty())
                .andExpect(jsonPath("$.assignee").isEmpty())
                .andExpect(jsonPath("$.dueDate").isEmpty());
        perform(member, get("/api/v1/cards/{id}", cardId))
                .andExpect(jsonPath("$.title").value("Final"))
                .andExpect(jsonPath("$.assignee").isEmpty());
    }

    @Test
    void editRequiresTheVersionAndATitle() throws Exception {
        Fixture f = fixture();
        UUID cardId = createCard(f.team().member(), f.columnId(), "Task");

        perform(f.team().member(), patch("/api/v1/cards/{id}", cardId), Map.of("title", "No version"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.version").isNotEmpty());
        perform(f.team().member(), patch("/api/v1/cards/{id}", cardId), fields("title", "", "version", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").isNotEmpty());
    }

    @Test
    void anyMemberDeletesCards() throws Exception {
        Fixture f = fixture();
        UUID keep = createCard(f.team().owner(), f.columnId(), "Keep");
        UUID remove = createCard(f.team().owner(), f.columnId(), "Remove");

        perform(f.team().member(), delete("/api/v1/cards/{id}", remove)).andExpect(status().isNoContent());

        perform(f.team().member(), get("/api/v1/cards/{id}", remove)).andExpect(status().isNotFound());
        perform(f.team().member(), get("/api/v1/cards/{id}", keep)).andExpect(status().isOk());
        assertThat(cardTitles(f.team().member(), f.boardId(), f.columnId())).containsExactly("Keep");
    }

    @Test
    void outsidersGetNotFound() throws Exception {
        Fixture f = fixture();
        TestUser mallory = registerUser("Mallory");
        UUID cardId = createCard(f.team().member(), f.columnId(), "Secret");

        perform(mallory, get("/api/v1/cards/{id}", cardId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Card not found"));
        perform(mallory, patch("/api/v1/cards/{id}", cardId), fields("title", "Hacked", "version", 0))
                .andExpect(status().isNotFound());
        moveCard(mallory, cardId, f.columnId(), null, null).andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/cards/{id}", cardId)).andExpect(status().isNotFound());

        // Same answer as for an id that really does not exist.
        perform(mallory, get("/api/v1/cards/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Card not found"));
        // And the card is untouched.
        perform(f.team().member(), get("/api/v1/cards/{id}", cardId)).andExpect(jsonPath("$.title").value("Secret"));
    }

    @Test
    void cardsAreUnassignedWhenTheirAssigneeLeavesTheWorkspace() throws Exception {
        Fixture f = fixture();
        TestUser owner = f.team().owner();
        TestUser member = f.team().member();
        TestUser admin = f.team().admin();

        // The member also belongs to a second workspace, where nothing must change.
        UUID otherWorkspace = createWorkspace(owner, "Other");
        addMember(owner, otherWorkspace, member, WorkspaceRole.MEMBER);
        UUID otherColumn = createColumn(owner, createBoard(owner, otherWorkspace, "Other board"), "To do");

        UUID removedMembersCard = createAssignedCard(owner, f.columnId(), member);
        UUID leavingAdminsCard = createAssignedCard(owner, f.columnId(), admin);
        UUID ownersCard = createAssignedCard(owner, f.columnId(), owner);
        UUID cardElsewhere = createAssignedCard(owner, otherColumn, member);

        // Removed by the owner...
        perform(owner, delete("/api/v1/workspaces/{id}/members/{userId}", f.team().workspaceId(), member.id()))
                .andExpect(status().isNoContent());
        // ...and leaving on their own.
        perform(admin, delete("/api/v1/workspaces/{id}/members/{userId}", f.team().workspaceId(), admin.id()))
                .andExpect(status().isNoContent());

        perform(owner, get("/api/v1/cards/{id}", removedMembersCard)).andExpect(jsonPath("$.assignee").isEmpty());
        perform(owner, get("/api/v1/cards/{id}", leavingAdminsCard)).andExpect(jsonPath("$.assignee").isEmpty());
        perform(owner, get("/api/v1/cards/{id}", ownersCard))
                .andExpect(jsonPath("$.assignee.id").value(owner.id().toString()));
        perform(owner, get("/api/v1/cards/{id}", cardElsewhere))
                .andExpect(jsonPath("$.assignee.id").value(member.id().toString()));
    }

    private UUID createAssignedCard(TestUser creator, UUID columnId, TestUser assignee) throws Exception {
        return idOf(perform(
                        creator,
                        post("/api/v1/columns/{id}/cards", columnId),
                        Map.of("title", "Task for " + assignee.displayName(), "assigneeId", assignee.id().toString()))
                .andExpect(status().isCreated()));
    }
}
