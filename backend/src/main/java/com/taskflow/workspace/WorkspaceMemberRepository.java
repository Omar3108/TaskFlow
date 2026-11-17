package com.taskflow.workspace;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserEmail(UUID workspaceId, String email);

    // "join fetch" loads the associated entity in the same SQL query. Without it, mapping a
    // list of N memberships would trigger N extra queries (the N+1 problem).

    @Query("""
            select m from WorkspaceMember m
            join fetch m.workspace w
            where m.user.id = :userId
            order by lower(w.name), w.createdAt
            """)
    List<WorkspaceMember> findAllByUserIdWithWorkspace(@Param("userId") UUID userId);

    @Query("""
            select m from WorkspaceMember m
            join fetch m.user u
            where m.workspace.id = :workspaceId
            order by m.joinedAt, u.email
            """)
    List<WorkspaceMember> findAllByWorkspaceIdWithUser(@Param("workspaceId") UUID workspaceId);
}
