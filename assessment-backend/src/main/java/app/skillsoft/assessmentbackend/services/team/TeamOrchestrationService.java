package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.team.*;

import java.util.UUID;

/**
 * Service interface for team orchestration operations.
 * Handles multi-step workflows with saga pattern for compensation.
 */
public interface TeamOrchestrationService {

    /**
     * Create team with members using saga pattern.
     * Handles rollback if any step fails.
     *
     * @param command The team creation command
     * @return Result containing the created team or error details
     */
    TeamCreationResult createTeamWithMembers(CreateTeamCommand command);

    /**
     * Add multiple members to a team atomically.
     * Uses partial success pattern - adds as many as possible.
     *
     * @param teamId The team ID
     * @param command The add members command
     * @return Result with added members and any failures
     */
    MemberAdditionResult addMembersToTeam(UUID teamId, AddMembersCommand command);

    /**
     * Change team leader with validation and audit.
     *
     * @param teamId The team ID
     * @param newLeaderId The new leader's user ID (null to remove leader)
     * @return Result of the leader change
     */
    LeaderChangeResult changeTeamLeader(UUID teamId, UUID newLeaderId);

    /**
     * Activate team with all validations.
     * Transitions from DRAFT to ACTIVE.
     *
     * @param teamId The team ID
     * @return Result of the activation
     */
    ActivationResult activateTeam(UUID teamId);

    /**
     * Archive team with member cleanup.
     * Transitions to ARCHIVED status.
     *
     * @param teamId The team ID
     * @return Result of the archive operation
     */
    ArchiveResult archiveTeam(UUID teamId);
}
