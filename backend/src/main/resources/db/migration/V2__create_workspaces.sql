CREATE TABLE workspaces (
    id         UUID        PRIMARY KEY,
    name       VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE workspace_members (
    id           UUID        PRIMARY KEY,
    workspace_id UUID        NOT NULL,
    user_id      UUID        NOT NULL,
    role         VARCHAR(20) NOT NULL,
    joined_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_workspace_members_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_workspace_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    -- A user belongs to a workspace at most once. This index also serves lookups by workspace.
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id),
    CONSTRAINT ck_workspace_members_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

-- "Which workspaces does this user belong to?"
CREATE INDEX idx_workspace_members_user_id ON workspace_members (user_id);

-- Exactly one owner per workspace, enforced by the database.
CREATE UNIQUE INDEX uq_workspace_members_single_owner ON workspace_members (workspace_id) WHERE role = 'OWNER';

CREATE TABLE invitations (
    id           UUID         PRIMARY KEY,
    workspace_id UUID         NOT NULL,
    email        VARCHAR(254) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    -- Only the SHA-256 hash of the invitation token is stored.
    token_hash   VARCHAR(64)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    invited_by   UUID         NOT NULL,
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_invitations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_invitations_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_invitations_role CHECK (role IN ('ADMIN', 'MEMBER')),
    CONSTRAINT ck_invitations_status CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED'))
);

-- "Which invitations are waiting for this e-mail address?"
CREATE INDEX idx_invitations_email ON invitations (email);

-- At most one pending invitation per e-mail and workspace. Also serves lookups by workspace.
CREATE UNIQUE INDEX uq_invitations_pending ON invitations (workspace_id, email) WHERE status = 'PENDING';
