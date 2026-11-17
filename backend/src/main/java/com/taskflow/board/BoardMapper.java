package com.taskflow.board;

import com.taskflow.board.dto.BoardResponse;
import com.taskflow.board.dto.ColumnResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BoardMapper {

    @Mapping(target = "workspaceId", source = "workspace.id")
    BoardResponse toBoardResponse(Board board);

    @Mapping(target = "boardId", source = "board.id")
    ColumnResponse toColumnResponse(BoardColumn column);
}
