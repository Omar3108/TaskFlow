package com.taskflow.board;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BoardRepository extends JpaRepository<Board, UUID> {

    List<Board> findAllByWorkspaceIdOrderByCreatedAtAscIdAsc(UUID workspaceId);
}
