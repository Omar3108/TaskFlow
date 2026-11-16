package com.taskflow.workspace;

import com.taskflow.workspace.dto.InvitationResponse;
import com.taskflow.workspace.dto.MemberResponse;
import com.taskflow.workspace.dto.ReceivedInvitationResponse;
import com.taskflow.workspace.dto.WorkspaceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkspaceMapper {

    /** A workspace as seen by one of its members: workspace data plus that member's role. */
    @Mapping(target = "id", source = "workspace.id")
    @Mapping(target = "name", source = "workspace.name")
    @Mapping(target = "createdAt", source = "workspace.createdAt")
    WorkspaceResponse toWorkspaceResponse(WorkspaceMember member);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "displayName", source = "user.displayName")
    MemberResponse toMemberResponse(WorkspaceMember member);

    @Mapping(target = "invitedByName", source = "invitedBy.displayName")
    InvitationResponse toInvitationResponse(Invitation invitation);

    @Mapping(target = "workspaceId", source = "workspace.id")
    @Mapping(target = "workspaceName", source = "workspace.name")
    @Mapping(target = "invitedByName", source = "invitedBy.displayName")
    ReceivedInvitationResponse toReceivedInvitationResponse(Invitation invitation);
}
