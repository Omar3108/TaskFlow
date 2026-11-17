package com.taskflow.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, UUID> {

    /** Loads the column together with its board: the board is needed to find the workspace. */
    @Query("select c from BoardColumn c join fetch c.board where c.id = :id")
    Optional<BoardColumn> findByIdWithBoard(@Param("id") UUID id);

    // The id is a tie-breaker: two columns created at the same instant by concurrent requests
    // can end up with the same position, and the order must still be the same on every read.
    List<BoardColumn> findAllByBoardIdOrderByPositionAscIdAsc(UUID boardId);

    @Query("select max(c.position) from BoardColumn c where c.board.id = :boardId")
    Double findMaxPositionByBoardId(@Param("boardId") UUID boardId);

    /**
     * Rewrites the positions of the columns of a board as STEP, 2 * STEP, ... keeping the
     * current order. One atomic statement; native because JPQL has no window functions. The
     * persistence context is flushed before and cleared after, because entities loaded earlier
     * would otherwise keep their old positions.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update board_columns set position = ranked.rn * :step
            from (select id, row_number() over (order by position, id) as rn
                  from board_columns where board_id = :boardId) ranked
            where board_columns.id = ranked.id
            """, nativeQuery = true)
    int rebalanceBoard(@Param("boardId") UUID boardId, @Param("step") double step);
}
