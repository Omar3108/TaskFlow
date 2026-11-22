package com.taskflow.card;

import com.taskflow.card.dto.AssigneeResponse;
import com.taskflow.card.dto.CardResponse;
import com.taskflow.label.LabelMapper;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

// Card summaries are assembled in BoardService, because their labels and comment counts come
// from separate bulk queries rather than from the entity.
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = LabelMapper.class)
public interface CardMapper {

    @Mapping(target = "boardId", source = "column.board.id")
    @Mapping(target = "columnId", source = "column.id")
    // labels: MapStruct picks LabelMapper.toSortedResponses for Set<Label> -> List<LabelResponse>.
    CardResponse toResponse(Card card);

    AssigneeResponse toAssignee(User user);
}
