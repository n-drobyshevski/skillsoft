package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.TeamDto;
import app.skillsoft.assessmentbackend.domain.dto.team.TeamMemberDto;
import app.skillsoft.assessmentbackend.domain.dto.team.TeamSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.team.UpdateTeamRequest;
import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for team read operations and simple updates.
 */
public interface TeamQueryService {

    /**
     * Find a team by ID.
     */
    Optional<Team> findById(UUID teamId);

    /**
     * Find a team by ID with members eagerly loaded.
     */
    Optional<Team> findByIdWithMembers(UUID teamId);

    /**
     * Get team as DTO.
     */
    Optional<TeamDto> getTeamDto(UUID teamId);

    /**
     * Find teams with optional status filter and search.
     */
    Page<TeamSummaryDto> findTeams(TeamStatus status, String search, Pageable pageable);

    /**
     * Find teams by member's Clerk ID.
     */
    List<TeamSummaryDto> findTeamsByMember(String clerkId);

    /**
     * Get team members.
     */
    List<TeamMemberDto> getTeamMembers(UUID teamId);

    /**
     * Update team name/description.
     */
    Optional<Team> updateTeam(UUID teamId, UpdateTeamRequest request);

    /**
     * Remove a member from a team.
     */
    boolean removeMember(UUID teamId, UUID userId);

    /**
     * Get team statistics.
     */
    TeamStatistics getStatistics();

    /**
     * Statistics record for teams.
     */
    record TeamStatistics(
            long totalTeams,
            long draftTeams,
            long activeTeams,
            long archivedTeams
    ) {}
}
