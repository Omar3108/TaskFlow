package com.taskflow.label;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LabelRepository extends JpaRepository<Label, UUID> {

    /** Loads the label with its board: the board is needed to find the workspace. */
    @Query("select l from Label l join fetch l.board where l.id = :id")
    Optional<Label> findByIdWithBoard(@Param("id") UUID id);

    /**
     * Alphabetical ignoring case, like the labels shown on each card. A plain ORDER BY name
     * depends on the database collation: with "C" (the PostgreSQL Docker default) "Urgent"
     * sorts before "bug".
     */
    @Query("select l from Label l where l.board.id = :boardId order by lower(l.name), l.id")
    List<Label> findAllByBoardIdAlphabetically(@Param("boardId") UUID boardId);

    /** Only the labels of this board: ids from another board are silently left out. */
    List<Label> findAllByBoardIdAndIdIn(UUID boardId, Collection<UUID> ids);

    boolean existsByBoardIdAndNameIgnoreCase(UUID boardId, String name);

    boolean existsByBoardIdAndNameIgnoreCaseAndIdNot(UUID boardId, String name, UUID id);
}
