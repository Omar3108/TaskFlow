CREATE TABLE labels (
    id         UUID        PRIMARY KEY,
    board_id   UUID        NOT NULL,
    name       VARCHAR(30) NOT NULL,
    -- A fixed palette (see LabelColor) instead of free hex values: consistent colours, and the
    -- frontend maps each value to a static CSS class.
    color      VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_labels_board FOREIGN KEY (board_id) REFERENCES boards (id) ON DELETE CASCADE,
    CONSTRAINT ck_labels_color CHECK (color IN ('GRAY', 'RED', 'ORANGE', 'YELLOW', 'GREEN', 'BLUE', 'PURPLE', 'PINK'))
);

-- Label names are unique per board regardless of case ("Bug" and "bug" are the same label).
-- An expression index: a plain UNIQUE (board_id, name) constraint would be case-sensitive.
-- It also serves "labels of this board".
CREATE UNIQUE INDEX uq_labels_board_name ON labels (board_id, lower(name));

-- Join table of the many-to-many between cards and labels.
CREATE TABLE card_labels (
    card_id  UUID NOT NULL,
    label_id UUID NOT NULL,
    -- The primary key also serves "labels of this card".
    CONSTRAINT pk_card_labels PRIMARY KEY (card_id, label_id),
    CONSTRAINT fk_card_labels_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_labels_label FOREIGN KEY (label_id) REFERENCES labels (id) ON DELETE CASCADE
);

-- Serves "cards with this label" (the filter) and the cascade when a label is deleted.
CREATE INDEX idx_card_labels_label_id ON card_labels (label_id);

CREATE TABLE comments (
    id         UUID          PRIMARY KEY,
    card_id    UUID          NOT NULL,
    -- Deleting a user account keeps the conversation; the comment shows "Deleted user".
    author_id  UUID,
    body       VARCHAR(2000) NOT NULL,
    created_at TIMESTAMPTZ   NOT NULL,
    updated_at TIMESTAMPTZ   NOT NULL,
    CONSTRAINT fk_comments_card FOREIGN KEY (card_id) REFERENCES cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_comments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE SET NULL
);

-- Serves "comments of this card, newest first" (the paginated list) and the comment counts.
CREATE INDEX idx_comments_card_created ON comments (card_id, created_at DESC);
