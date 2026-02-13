package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TeamMember;
import app.skillsoft.assessmentbackend.domain.entities.User;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Mapper for converting between Team entities and DTOs.
 */
@Component
public class TeamMapper {

    /**
     * Converts a CreateTeamRequest to CreateTeamCommand with creator ID.
     */
    public CreateTeamCommand toCommand(CreateTeamRequest request, String creatorClerkId, UUID creatorUserId) {
        return new CreateTeamCommand(
                request.name(),
                request.description(),
                request.memberIds(),
                request.leaderId(),
                request.activateImmediately(),
                creatorUserId
        );
    }

    /**
     * Converts a Team entity to TeamDto.
     */
    public TeamDto toDto(Team team) {
        List<TeamMemberDto> memberDtos = team.getMembers().stream()
                .filter(TeamMember::isActive)
                .map(this::toMemberDto)
                .toList();

        return new TeamDto(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getStatus(),
                team.getLeader() != null ? toUserSummary(team.getLeader()) : null,
                toUserSummary(team.getCreatedBy()),
                memberDtos,
                team.getActiveMemberCount(),
                team.getCreatedAt(),
                team.getActivatedAt(),
                team.getArchivedAt()
        );
    }

    /**
     * Converts a Team entity to TeamSummaryDto.
     */
    public TeamSummaryDto toSummaryDto(Team team) {
        return new TeamSummaryDto(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getStatus(),
                team.getLeader() != null ? toUserSummary(team.getLeader()) : null,
                team.getActiveMemberCount(),
                team.getCreatedAt()
        );
    }

    /**
     * Converts a TeamMember entity to TeamMemberDto.
     */
    public TeamMemberDto toMemberDto(TeamMember member) {
        User user = member.getUser();
        return new TeamMemberDto(
                user.getId(),
                user.getClerkId(),
                user.getFullName(),
                user.getEmail(),
                user.getImageUrl(),
                member.getRole(),
                member.getJoinedAt(),
                member.isActive()
        );
    }

    /**
     * Converts a User entity to UserSummaryDto.
     */
    public UserSummaryDto toUserSummary(User user) {
        if (user == null) return null;
        return new UserSummaryDto(
                user.getId(),
                user.getClerkId(),
                user.getFullName(),
                user.getEmail(),
                user.getImageUrl()
        );
    }

    /**
     * Converts MemberAdditionResult to API DTO.
     */
    public MemberAdditionResultDto toDto(MemberAdditionResult result) {
        return new MemberAdditionResultDto(
                result.addedMembers(),
                result.failures(),
                result.hasErrors()
        );
    }

    /**
     * Converts LeaderChangeResult to API DTO.
     */
    public LeaderChangeResultDto toDto(LeaderChangeResult result) {
        return new LeaderChangeResultDto(
                result.success(),
                result.previousLeaderId(),
                result.newLeaderId()
        );
    }
}
