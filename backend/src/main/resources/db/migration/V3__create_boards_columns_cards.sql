CREATE TABLE boards (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    name         VARCHAR(80)  NOT NULL,
    description  VARCHAR(500),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_boards_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE
);

-- "Which boards does this workspace have?"
CREATE INDEX idx_boards_workspace_id ON boards (workspace_id);

-- Named board_columns because COLUMN is a reserved SQL word.
CREATE TABLE board_columns (
    id         UUID             PRIMARY KEY,
    board_id   UUID             NOT NULL,
    name       VARCHAR(60)      NOT NULL,
    -- Fractional index: an item placed between two others gets a value between theirs, so a
    -- move updates one row instead of renumbering the whole list.
    position   DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL,
    updated_at TIMESTAMPTZ      NOT NULL,
    CONSTRAINT fk_board_columns_board FOREIGN KEY (board_id) REFERENCES boards (id) ON DELETE CASCADE
);

-- Serves "columns of this board, in order" without a separate sort step.
CREATE INDEX idx_board_columns_board_position ON board_columns (board_id, position);

CREATE TABLE cards (
    id          UUID             PRIMARY KEY,
    column_id   UUID             NOT NULL,
    title       VARCHAR(200)     NOT NULL,
    description TEXT,
    position    DOUBLE PRECISION NOT NULL,
    assignee_id UUID,
    due_date    DATE,
    created_by  UUID,
    -- Optimistic-locking counter: incremented on every update of the card.
    version     BIGINT           NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ      NOT NULL,
    updated_at  TIMESTAMPTZ      NOT NULL,
    CONSTRAINT fk_cards_column FOREIGN KEY (column_id) REFERENCES board_columns (id) ON DELETE CASCADE,
    -- Deleting a user account must not delete the team's cards.
    CONSTRAINT fk_cards_assignee FOREIGN KEY (assignee_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT fk_cards_created_by FOREIGN KEY (created_by) REFERENCES users (id) ON DELETE SET NULL
);

-- Serves "cards of this column, in order".
CREATE INDEX idx_cards_column_position ON cards (column_id, position);

-- Serves the clean-up when a member leaves, and later "cards assigned to me".
CREATE INDEX idx_cards_assignee_id ON cards (assignee_id);
