package com.taskflow.board;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.taskflow.workspace.WorkspaceRole;
import com.taskflow.workspace.WorkspaceTestSupport;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.test.web.servlet.ResultActions;

/** Shared steps for board, column and card tests, all performed through the public API. */
public abstract class BoardTestSupport extends WorkspaceTestSupport {

    /** A workspace with one user of each role. */
    public record Team(UUID workspaceId, TestUser owner, TestUser admin, TestUser member) {
    }

    protected Team createTeam() throws Exception {
        TestUser owner = registerUser("Owner");
        TestUser admin = registerUser("Admin");
        TestUser member = registerUser("Member");
        UUID workspaceId = createWorkspace(owner, "Team");
        addMember(owner, workspaceId, admin, WorkspaceRole.ADMIN);
        addMember(owner, workspaceId, member, WorkspaceRole.MEMBER);
        return new Team(workspaceId, owner, admin, member);
    }

    /** An empty board (no default columns), so tests control exactly which columns exist. */
    protected UUID createBoard(TestUser user, UUID workspaceId, String name) throws Exception {
        return idOf(perform(
                        user,
                        post("/api/v1/workspaces/{id}/boards", workspaceId),
                        Map.of("name", name, "defaultColumns", false))
                .andExpect(status().isCreated()));
    }

    protected UUID createColumn(TestUser user, UUID boardId, String name) throws Exception {
        return idOf(perform(user, post("/api/v1/boards/{id}/columns", boardId), Map.of("name", name))
                .andExpect(status().isCreated()));
    }

    protected UUID createCard(TestUser user, UUID columnId, String title) throws Exception {
        return idOf(perform(user, post("/api/v1/columns/{id}/cards", columnId), Map.of("title", title))
                .andExpect(status().isCreated()));
    }

    protected ResultActions moveColumn(TestUser user, UUID columnId, UUID previousColumnId, UUID nextColumnId)
            throws Exception {
        return perform(
                user,
                patch("/api/v1/columns/{id}/move", columnId),
                fields("previousColumnId", previousColumnId, "nextColumnId", nextColumnId));
    }

    protected ResultActions moveCard(TestUser user, UUID cardId, UUID columnId, UUID previousCardId, UUID nextCardId)
            throws Exception {
        return perform(
                user,
                patch("/api/v1/cards/{id}/move", cardId),
                fields("columnId", columnId, "previousCardId", previousCardId, "nextCardId", nextCardId));
    }

    /** Column names in the order the board returns them. */
    protected List<String> columnNames(TestUser user, UUID boardId) throws Exception {
        List<String> names = new ArrayList<>();
        board(user, boardId).get("columns").forEach(column -> names.add(column.get("name").asText()));
        return names;
    }

    /** Card titles of one column in the order the board returns them. */
    protected List<String> cardTitles(TestUser user, UUID boardId, UUID columnId) throws Exception {
        for (JsonNode column : board(user, boardId).get("columns")) {
            if (column.get("id").asText().equals(columnId.toString())) {
                List<String> titles = new ArrayList<>();
                column.get("cards").forEach(card -> titles.add(card.get("title").asText()));
                return titles;
            }
        }
        throw new AssertionError("Column " + columnId + " is not part of board " + boardId);
    }

    protected JsonNode board(TestUser user, UUID boardId) throws Exception {
        return body(perform(user, get("/api/v1/boards/{id}", boardId)).andExpect(status().isOk()));
    }

    protected UUID idOf(ResultActions actions) throws Exception {
        return UUID.fromString(body(actions).get("id").asText());
    }

    /** A JSON object from key/value pairs. Unlike Map.of, it accepts null values. */
    protected static Map<String, Object> fields(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }
        return map;
    }
}
