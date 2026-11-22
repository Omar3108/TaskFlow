package com.taskflow.comment;

import com.taskflow.comment.dto.CommentResponse;
import com.taskflow.comment.dto.CommentResponse.CommentAuthorResponse;
import com.taskflow.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CommentMapper {

    @Mapping(target = "cardId", source = "card.id")
    CommentResponse toResponse(Comment comment);

    CommentAuthorResponse toAuthor(User user);
}
