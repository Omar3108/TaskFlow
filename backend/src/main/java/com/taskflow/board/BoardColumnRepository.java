package com.taskflow.board;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
