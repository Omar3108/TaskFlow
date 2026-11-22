package com.taskflow.comment;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.taskflow.board.BoardTestSupport;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommentIntegrationTest extends BoardTestSupport {

    private Team team;
    private UUID boardId;
    private UUID columnId;
    private UUID cardId;

    @BeforeEach
    void createCard() throws Exception {
        team = createTeam();
        boardId = createBoard(team.owner(), team.workspaceId(), "Roadmap");
        columnId = createColumn(team.owner(), boardId, "To do");
        cardId = createCard(team.member(), columnId, "Task");
    }

    @Test
    void anyMemberComments() throws Exception {
        perform(team.member(), post("/api/v1/cards/{id}/comments", cardId), Map.of("body", "  Looks good  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.body").value("Looks good"))
                .andExpect(jsonPath("$.author.id").value(team.member().id().toString()))
                .andExpect(jsonPath("$.author.displayName").value("Member"))
                .andExpect(jsonPath("$.edited").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void bodyIsRequiredAndLimited() throws Exception {
        perform(team.member(), post("/api/v1/cards/{id}/comments", cardId), Map.of("body", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.body").isNotEmpty());
        perform(team.member(), post("/api/v1/cards/{id}/comments", cardId), Map.of("body", "x".repeat(2001)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listIsPaginatedNewestFirst() throws Exception {
        for (int i = 1; i <= 5; i++) {
            comment(team.member(), cardId, "Comment " + i);
        }

        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId).param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].body").value(contains("Comment 5", "Comment 4")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId).param("size", "2").param("page", "2"))
                .andExpect(jsonPath("$.content[*].body").value(contains("Comment 1")));
        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId).param("sort", "createdAt,asc"))
                .andExpect(jsonPath("$.content[0].body").value("Comment 1"));
    }

    @Test
    void sortingByOtherFieldsIsRejected() throws Exception {
        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId).param("sort", "body"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Comments can only be sorted by createdAt"));
    }

    @Test
    void pageSizeIsCapped() throws Exception {
        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId).param("size", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    @Test
    void onlyTheAuthorEditsAndTheCommentIsMarkedAsEdited() throws Exception {
        UUID commentId = comment(team.member(), cardId, "Frist");

        perform(team.member(), patch("/api/v1/comments/{id}", commentId), Map.of("body", "First"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("First"))
                .andExpect(jsonPath("$.edited").value(true));

        // Not even the owner can change someone else's words.
        for (TestUser other : new TestUser[] {team.owner(), team.admin()}) {
            perform(other, patch("/api/v1/comments/{id}", commentId), Map.of("body", "Changed"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("Only the author can edit a comment"));
        }
    }

    @Test
    void theAuthorOrAnAdminDeletesButNotAnotherMember() throws Exception {
        TestUser otherMember = registerUser("Other member");
        addMember(team.owner(), team.workspaceId(), otherMember, com.taskflow.workspace.WorkspaceRole.MEMBER);
        UUID byMember = comment(team.member(), cardId, "Mine");
        UUID moderated = comment(team.member(), cardId, "Spam");
        UUID byAdmin = comment(team.admin(), cardId, "Admin note");

        perform(otherMember, delete("/api/v1/comments/{id}", byMember))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only the author or an admin can delete a comment"));
        perform(team.member(), delete("/api/v1/comments/{id}", byAdmin)).andExpect(status().isForbidden());

        perform(team.member(), delete("/api/v1/comments/{id}", byMember)).andExpect(status().isNoContent());
        perform(team.admin(), delete("/api/v1/comments/{id}", moderated)).andExpect(status().isNoContent());

        perform(team.owner(), get("/api/v1/cards/{id}/comments", cardId))
                .andExpect(jsonPath("$.content[*].body").value(contains("Admin note")));
    }

    @Test
    void theBoardShowsHowManyCommentsEachCardHas() throws Exception {
        UUID quiet = createCard(team.member(), columnId, "Quiet");
        comment(team.member(), cardId, "One");
        comment(team.owner(), cardId, "Two");

        perform(team.member(), get("/api/v1/boards/{id}", boardId))
                .andExpect(jsonPath("$.columns[0].cards[?(@.id == '%s')].commentCount", cardId).value(contains(2)))
                .andExpect(jsonPath("$.columns[0].cards[?(@.id == '%s')].commentCount", quiet).value(contains(0)));
    }

    @Test
    void deletingTheCardDeletesItsComments() throws Exception {
        UUID commentId = comment(team.member(), cardId, "Bye");

        perform(team.member(), delete("/api/v1/cards/{id}", cardId)).andExpect(status().isNoContent());

        perform(team.member(), patch("/api/v1/comments/{id}", commentId), Map.of("body", "Still here?"))
                .andExpect(status().isNotFound());
    }

    @Test
    void outsidersGetNotFound() throws Exception {
        TestUser mallory = registerUser("Mallory");
        UUID commentId = comment(team.member(), cardId, "Private");

        perform(mallory, get("/api/v1/cards/{id}/comments", cardId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Card not found"));
        perform(mallory, post("/api/v1/cards/{id}/comments", cardId), Map.of("body", "Hi"))
                .andExpect(status().isNotFound());
        perform(mallory, patch("/api/v1/comments/{id}", commentId), Map.of("body", "Hacked"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Comment not found"));
        perform(mallory, delete("/api/v1/comments/{id}", commentId)).andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/comments/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Comment not found"));
    }
}
