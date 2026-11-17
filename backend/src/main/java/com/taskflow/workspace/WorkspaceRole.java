package com.taskflow.workspace;

/**
 * Role of a user inside one workspace. Roles are hierarchical: each one can do everything the
 * roles below it can.
 */
public enum WorkspaceRole {
    MEMBER(1),
    ADMIN(2),
    OWNER(3);

    // Explicit rank instead of ordinal(): reordering the constants cannot change permissions.
    private final int rank;

    WorkspaceRole(int rank) {
        this.rank = rank;
    }

    public boolean isAtLeast(WorkspaceRole minimum) {
        return rank >= minimum.rank;
    }
}
