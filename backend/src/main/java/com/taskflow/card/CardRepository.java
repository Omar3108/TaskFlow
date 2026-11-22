package com.taskflow.card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// JpaSpecificationExecutor adds findAll(Specification, Sort), used by the filtered board view.
public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

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

    /**
     * Every (card, label) pair of a board in one query. Mapping c.labels card by card would
     * run one query per card (N+1).
     */
    @Query("select new com.taskflow.card.CardLabelRow(c.id, l) from Card c join c.labels l where c.column.board.id = :boardId")
    List<CardLabelRow> findLabelsByBoardId(@Param("boardId") UUID boardId);

    List<Card> findAllByColumnIdOrderByPositionAscIdAsc(UUID columnId);

    @Query("select max(c.position) from Card c where c.column.id = :columnId")
    Double findMaxPositionByColumnId(@Param("columnId") UUID columnId);

    /**
     * Rewrites the positions of a column as STEP, 2 * STEP, ... keeping the current order
     * (position, then id, the same order every read uses). One statement, so it is atomic and
     * does not load the cards.
     *
     * <p>Native on purpose: JPQL has no window functions. It deliberately bypasses the version
     * column: a rebalance changes no content, so cards that somebody has open for editing must
     * stay editable, and it must not fail because a card of the column was edited meanwhile.
     * The persistence context is flushed before and cleared after, because the entities already
     * loaded would otherwise hold stale positions.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update cards set position = ranked.rn * :step
            from (select id, row_number() over (order by position, id) as rn
                  from cards where column_id = :columnId) ranked
            where cards.id = ranked.id
            """, nativeQuery = true)
    int rebalanceColumn(@Param("columnId") UUID columnId, @Param("step") double step);

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
