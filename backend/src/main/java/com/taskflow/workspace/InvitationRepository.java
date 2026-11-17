package com.taskflow.workspace;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends JpaRepository<Invitation, UUID> {

    @Query("""
            select i from Invitation i
            join fetch i.workspace
            join fetch i.invitedBy
            where i.tokenHash = :tokenHash
            """)
    Optional<Invitation> findByTokenHashWithDetails(@Param("tokenHash") String tokenHash);

    Optional<Invitation> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Query("""
            select i from Invitation i
            join fetch i.workspace
            where i.id = :id and i.email = :email
            """)
    Optional<Invitation> findByIdAndEmailWithWorkspace(@Param("id") UUID id, @Param("email") String email);

    Optional<Invitation> findByWorkspaceIdAndEmailAndStatus(UUID workspaceId, String email, InvitationStatus status);

    /** Invitations of a workspace that can still be accepted, newest first. */
    @Query("""
            select i from Invitation i
            join fetch i.invitedBy
            where i.workspace.id = :workspaceId
              and i.status = com.taskflow.workspace.InvitationStatus.PENDING
              and i.expiresAt > :now
            order by i.createdAt desc
            """)
    List<Invitation> findUsableByWorkspaceId(@Param("workspaceId") UUID workspaceId, @Param("now") Instant now);

    /** Invitations addressed to an e-mail that can still be accepted, newest first. */
    @Query("""
            select i from Invitation i
            join fetch i.workspace
            join fetch i.invitedBy
            where i.email = :email
              and i.status = com.taskflow.workspace.InvitationStatus.PENDING
              and i.expiresAt > :now
            order by i.createdAt desc
            """)
    List<Invitation> findUsableByEmail(@Param("email") String email, @Param("now") Instant now);
}
