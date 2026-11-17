package com.taskflow.card;

import com.taskflow.card.dto.AssigneeResponse;
import com.taskflow.card.dto.CardResponse;
import com.taskflow.card.dto.CardSummaryResponse;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardMapper {

    @Mapping(target = "boardId", source = "column.board.id")
    @Mapping(target = "columnId", source = "column.id")
    CardResponse toResponse(Card card);

    @Mapping(target = "columnId", source = "column.id")
    CardSummaryResponse toSummary(Card card);

    AssigneeResponse toAssignee(User user);
}
