package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.TeamDto;
import app.skillsoft.assessmentbackend.domain.dto.team.TeamMemberDto;
import app.skillsoft.assessmentbackend.domain.dto.team.TeamSummaryDto;
import app.skillsoft.assessmentbackend.domain.dto.team.UpdateTeamRequest;
import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TeamMember;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of TeamQueryService for read operations.
 */
@Service
@Transactional(readOnly = true)
public class TeamQueryServiceImpl implements TeamQueryService {

    private static final Logger log = LoggerFactory.getLogger(TeamQueryServiceImpl.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamMapper teamMapper;

    public TeamQueryServiceImpl(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TeamMapper teamMapper) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamMapper = teamMapper;
    }

    @Override
    public Optional<Team> findById(UUID teamId) {
        return teamRepository.findById(teamId);
    }

    @Override
    public Optional<Team> findByIdWithMembers(UUID teamId) {
        return teamRepository.findByIdWithMembers(teamId);
    }

    @Override
    public Optional<TeamDto> getTeamDto(UUID teamId) {
        return findByIdWithMembers(teamId)
                .map(teamMapper::toDto);
    }

    @Override
    public Page<TeamSummaryDto> findTeams(TeamStatus status, String search, Pageable pageable) {
        Page<Team> teams;

        if (search != null && !search.isBlank()) {
            teams = teamRepository.searchByNameAndStatus(search, status, pageable);
        } else if (status != null) {
            teams = teamRepository.findByStatus(status, pageable);
        } else {
            teams = teamRepository.findAll(pageable);
        }

        return teams.map(teamMapper::toSummaryDto);
    }

    @Override
    public List<TeamSummaryDto> findTeamsByMember(String clerkId) {
        return teamRepository.findByMemberClerkId(clerkId).stream()
                .map(teamMapper::toSummaryDto)
                .toList();
    }

    @Override
    public List<TeamMemberDto> getTeamMembers(UUID teamId) {
        return teamMemberRepository.findByTeamIdWithUser(teamId).stream()
                .map(teamMapper::toMemberDto)
                .toList();
    }

    @Override
    @Transactional
    public Optional<Team> updateTeam(UUID teamId, UpdateTeamRequest request) {
        return teamRepository.findById(teamId)
                .map(team -> {
                    if (team.isArchived()) {
                        throw new IllegalStateException("Cannot update archived team");
                    }

                    if (request.name() != null && !request.name().isBlank()) {
                        team.setName(request.name());
                    }
                    if (request.description() != null) {
                        team.setDescription(request.description());
                    }

                    log.info("Updated team {}: name={}", teamId, team.getName());
                    return teamRepository.save(team);
                });
    }

    @Override
    @Transactional
    public boolean removeMember(UUID teamId, UUID userId) {
        Optional<TeamMember> memberOpt = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);

        if (memberOpt.isEmpty() || !memberOpt.get().isActive()) {
            return false;
        }

        TeamMember member = memberOpt.get();
        Team team = member.getTeam();

        // If removing the leader, clear the leader reference
        if (team.getLeader() != null && team.getLeader().getId().equals(userId)) {
            team.setLeader(null);
            teamRepository.save(team);
        }

        member.remove();
        teamMemberRepository.save(member);

        log.info("Removed member {} from team {}", userId, teamId);
        return true;
    }

    @Override
    public TeamStatistics getStatistics() {
        long total = teamRepository.count();
        long draft = teamRepository.countByStatus(TeamStatus.DRAFT);
        long active = teamRepository.countByStatus(TeamStatus.ACTIVE);
        long archived = teamRepository.countByStatus(TeamStatus.ARCHIVED);

        return new TeamStatistics(total, draft, active, archived);
    }
}
