package com.taskflow.workspace;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    /**
     * Workspaces owned by expired demo accounts, in one statement. Boards, columns, cards,
     * labels, comments, members and invitations go with them through ON DELETE CASCADE.
     */
    @Modifying
    @Query("""
            delete from Workspace w where w.id in (
                select m.workspace.id from WorkspaceMember m
                where m.role = com.taskflow.workspace.WorkspaceRole.OWNER and m.user.demoExpiresAt < :now)
            """)
    int deleteOwnedByExpiredDemoAccounts(@Param("now") Instant now);
}
