package com.taskflow.card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID> {

    /**
     * Loads the card with everything a single-card operation needs (column and board to find
     * the workspace, assignee for the response) in one query.
     */
    @Query("""
            select c from Card c
            join fetch c.column col
            join fetch col.board
            left join fetch c.assignee
            where c.id = :id
            """)
    Optional<Card> findByIdWithBoard(@Param("id") UUID id);

    /** Every card of a board in display order: one query, however many columns there are. */
    @Query("""
            select c from Card c
            left join fetch c.assignee
            where c.column.board.id = :boardId
            order by c.position, c.id
            """)
    List<Card> findAllByBoardIdWithAssignee(@Param("boardId") UUID boardId);

    List<Card> findAllByColumnIdOrderByPositionAscIdAsc(UUID columnId);

    @Query("select max(c.position) from Card c where c.column.id = :columnId")
    Double findMaxPositionByColumnId(@Param("columnId") UUID columnId);

    /**
     * Bulk update: one SQL statement, no entities loaded. Note that bulk statements bypass the
     * persistence context, {@code @PreUpdate} and {@code @Version}.
     */
    @Modifying
    @Query("""
            update Card c set c.assignee = null
            where c.assignee.id = :userId
              and c.column.id in (select col.id from BoardColumn col where col.board.workspace.id = :workspaceId)
            """)
    int unassignAllInWorkspace(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);
}
