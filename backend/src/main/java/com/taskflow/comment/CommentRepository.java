package com.taskflow.comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    /**
     * One page of the comments of a card. The entity graph loads each author in the same
     * query; join fetch cannot be used here because Spring Data also derives a count query for
     * the page, and a count query cannot fetch.
     */
    @EntityGraph(attributePaths = "author")
    Page<Comment> findByCardId(UUID cardId, Pageable pageable);

    /** Loads the comment with what an operation on it needs: the chain up to the workspace. */
    @Query("""
            select cm from Comment cm
            join fetch cm.card c
            join fetch c.column col
            join fetch col.board
            left join fetch cm.author
            where cm.id = :id
            """)
    Optional<Comment> findByIdWithCard(@Param("id") UUID id);

    /** Comment counts of every card of a board in one grouped query (cards without comments are absent). */
    @Query("""
            select new com.taskflow.comment.CommentCount(cm.card.id, count(cm))
            from Comment cm
            where cm.card.column.board.id = :boardId
            group by cm.card.id
            """)
    List<CommentCount> countByBoardId(@Param("boardId") UUID boardId);
}
