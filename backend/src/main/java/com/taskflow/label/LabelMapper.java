package com.taskflow.label;

import com.taskflow.label.dto.LabelResponse;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface LabelMapper {

    LabelResponse toResponse(Label label);

    /** Labels are always shown in alphabetical order, whatever order they were stored in. */
    default List<LabelResponse> toSortedResponses(Collection<Label> labels) {
        return labels.stream()
                .sorted(Comparator.comparing(Label::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }
}
