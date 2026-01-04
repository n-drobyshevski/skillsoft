package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.services.team.saga.SagaContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of TeamOrchestrationService with saga pattern.
 * Handles multi-step team operations with compensation on failure.
 */
@Service
@Transactional
public class TeamOrchestrationServiceImpl implements TeamOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(TeamOrchestrationServiceImpl.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;

    public TeamOrchestrationServiceImpl(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            UserRepository userRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TeamCreationResult createTeamWithMembers(CreateTeamCommand command) {
        SagaContext context = new SagaContext();
        List<String> warnings = new ArrayList<>();

        try {
            // Step 1: Validate creator exists
            User creator = userRepository.findById(command.createdById())
                    .orElseThrow(() -> new IllegalArgumentException("Creator not found: " + command.createdById()));

            // Step 2: Create team in DRAFT status
            Team team = new Team(command.name(), command.description(), creator);
            team = teamRepository.save(team);
            context.setTeamId(team.getId());
            context.recordStep("TEAM_CREATED");
            log.info("Created team {} in DRAFT status", team.getId());

            // Step 3: Add members (with partial failure handling)
            for (UUID memberId : command.memberIds()) {
                try {
                    User member = userRepository.findById(memberId)
                            .orElseThrow(() -> new IllegalArgumentException("Member not found: " + memberId));

                    if (!member.isActive()) {
                        warnings.add("Skipped inactive user: " + memberId);
                        continue;
                    }

                    TeamMember teamMember = new TeamMember(team, member, TeamMemberRole.MEMBER);
                    teamMemberRepository.save(teamMember);
                    team.getMembers().add(teamMember);
                    context.addMemberId(memberId);
                } catch (Exception e) {
                    warnings.add("Failed to add member " + memberId + ": " + e.getMessage());
                    log.warn("Failed to add member {} to team {}: {}", memberId, team.getId(), e.getMessage());
                }
            }
            context.recordStep("MEMBERS_ADDED");

            // Step 4: Set leader if specified and valid
            if (command.leaderId() != null) {
                try {
                    boolean isLeaderMember = context.getAddedMemberIds().contains(command.leaderId());
                    if (isLeaderMember) {
                        User leader = userRepository.findById(command.leaderId())
                                .orElseThrow(() -> new IllegalArgumentException("Leader not found"));

                        team.setLeader(leader);

                        // Update member role to LEADER
                        teamMemberRepository.findByTeamIdAndUserId(team.getId(), command.leaderId())
                                .ifPresent(TeamMember::promoteToLeader);

                        context.setLeaderId(command.leaderId());
                        context.recordStep("LEADER_SET");
                    } else {
                        warnings.add("Specified leader is not a team member, skipped leader assignment");
                    }
                } catch (Exception e) {
                    warnings.add("Failed to set leader: " + e.getMessage());
                }
            }

            // Step 5: Optionally activate immediately
            if (command.activateImmediately() && team.getActiveMemberCount() > 0) {
                team.activate();
                context.recordStep("TEAM_ACTIVATED");
            }

            // Save final state
            team = teamRepository.save(team);

            log.info("Team creation saga completed successfully for team {}", team.getId());
            return TeamCreationResult.success(team, warnings);

        } catch (Exception e) {
            log.error("Team creation saga failed, initiating compensation", e);
            compensate(context);
            return TeamCreationResult.failure(e.getMessage(), context.getErrors());
        }
    }

    @Override
    public MemberAdditionResult addMembersToTeam(UUID teamId, AddMembersCommand command) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        if (team.isArchived()) {
            return MemberAdditionResult.failure("Cannot add members to archived team");
        }

        List<UUID> addedMembers = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        for (UUID userId : command.userIds()) {
            try {
                // Check if already a member
                if (teamMemberRepository.existsByTeamIdAndUserIdAndIsActiveTrue(teamId, userId)) {
                    failures.add("User " + userId + " is already an active member");
                    continue;
                }

                User user = userRepository.findById(userId)
                        .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

                if (!user.isActive()) {
                    failures.add("User " + userId + " is not active");
                    continue;
                }

                // Check for reactivation of previous member
                var existingMember = teamMemberRepository.findByTeamIdAndUserId(teamId, userId);
                if (existingMember.isPresent()) {
                    existingMember.get().reactivate();
                    teamMemberRepository.save(existingMember.get());
                } else {
                    TeamMember newMember = new TeamMember(team, user, TeamMemberRole.MEMBER);
                    teamMemberRepository.save(newMember);
                }

                addedMembers.add(userId);
            } catch (Exception e) {
                failures.add("Failed to add user " + userId + ": " + e.getMessage());
            }
        }

        log.info("Added {} members to team {} (failures: {})", addedMembers.size(), teamId, failures.size());
        return MemberAdditionResult.partialSuccess(addedMembers, failures);
    }

    @Override
    public LeaderChangeResult changeTeamLeader(UUID teamId, UUID newLeaderId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        if (team.isArchived()) {
            return LeaderChangeResult.failure("Cannot change leader of archived team");
        }

        UUID previousLeaderId = team.getLeader() != null ? team.getLeader().getId() : null;

        // Demote current leader if exists
        if (team.getLeader() != null) {
            teamMemberRepository.findByTeamIdAndUserId(teamId, team.getLeader().getId())
                    .ifPresent(member -> {
                        member.demoteToMember();
                        teamMemberRepository.save(member);
                    });
        }

        // Set new leader
        if (newLeaderId != null) {
            User newLeader = userRepository.findById(newLeaderId)
                    .orElseThrow(() -> new IllegalArgumentException("New leader not found: " + newLeaderId));

            TeamMember leaderMember = teamMemberRepository.findByTeamIdAndUserId(teamId, newLeaderId)
                    .orElseThrow(() -> new IllegalArgumentException("New leader must be a team member"));

            if (!leaderMember.isActive()) {
                return LeaderChangeResult.failure("New leader must be an active member");
            }

            team.setLeader(newLeader);
            leaderMember.promoteToLeader();
            teamMemberRepository.save(leaderMember);
        } else {
            team.setLeader(null);
        }

        teamRepository.save(team);
        log.info("Changed leader for team {} from {} to {}", teamId, previousLeaderId, newLeaderId);

        return LeaderChangeResult.success(previousLeaderId, newLeaderId);
    }

    @Override
    public ActivationResult activateTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        List<String> errors = validateForActivation(team);
        if (!errors.isEmpty()) {
            return ActivationResult.failure(errors);
        }

        team.activate();
        teamRepository.save(team);

        log.info("Activated team {}", teamId);
        return ActivationResult.success(team.getActivatedAt());
    }

    @Override
    public ArchiveResult archiveTeam(UUID teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found: " + teamId));

        if (team.isArchived()) {
            return ArchiveResult.failure("Team is already archived");
        }

        team.archive();
        teamRepository.save(team);

        // Deactivate all members
        teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId)
                .forEach(member -> {
                    member.remove();
                    teamMemberRepository.save(member);
                });

        log.info("Archived team {}", teamId);
        return ArchiveResult.success(team.getArchivedAt());
    }

    // Compensation Logic
    private void compensate(SagaContext context) {
        log.info("Executing compensation for saga context: {}", context.getCompletedSteps());

        try {
            if (context.getTeamId() != null) {
                // Delete team (cascades to members via orphanRemoval)
                teamRepository.deleteById(context.getTeamId());
                log.info("Compensation: Deleted team {}", context.getTeamId());
            }
        } catch (Exception e) {
            log.error("Compensation failed for team {}: {}", context.getTeamId(), e.getMessage());
            context.recordError("Compensation failed: " + e.getMessage());
        }
    }

    private List<String> validateForActivation(Team team) {
        List<String> errors = new ArrayList<>();

        if (team.getStatus() != TeamStatus.DRAFT) {
            errors.add("Only DRAFT teams can be activated, current status: " + team.getStatus());
        }

        if (team.getActiveMemberCount() == 0) {
            errors.add("Team must have at least one active member");
        }

        return errors;
    }
}
