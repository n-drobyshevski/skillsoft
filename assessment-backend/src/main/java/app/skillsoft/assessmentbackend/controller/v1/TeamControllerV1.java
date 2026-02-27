package app.skillsoft.assessmentbackend.controller.v1;

import app.skillsoft.assessmentbackend.domain.dto.team.*;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import app.skillsoft.assessmentbackend.repository.UserRepository;
import app.skillsoft.assessmentbackend.security.SessionSecurityService;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import app.skillsoft.assessmentbackend.services.team.TeamMapper;
import app.skillsoft.assessmentbackend.services.team.TeamOrchestrationService;
import app.skillsoft.assessmentbackend.services.team.TeamQueryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * V1 REST Controller for Team management operations.
 *
 * Provides endpoints for team CRUD operations, member management,
 * and team profile/fit score calculations for TEAM_FIT assessments.
 *
 * Security:
 * - All endpoints require ADMIN role
 * - Uses @PreAuthorize for method-level security
 *
 * API Base Path: /api/v1/teams
 */
@RestController
@RequestMapping("/api/v1/teams")
@PreAuthorize("hasRole('ADMIN')")
public class TeamControllerV1 {

    private static final Logger logger = LoggerFactory.getLogger(TeamControllerV1.class);

    private final TeamQueryService queryService;
    private final TeamOrchestrationService orchestrationService;
    private final TeamService teamService;
    private final TeamMapper teamMapper;
    private final UserRepository userRepository;
    private final SessionSecurityService sessionSecurity;

    public TeamControllerV1(
            TeamQueryService queryService,
            TeamOrchestrationService orchestrationService,
            TeamService teamService,
            TeamMapper teamMapper,
            UserRepository userRepository,
            SessionSecurityService sessionSecurity) {
        this.queryService = queryService;
        this.orchestrationService = orchestrationService;
        this.teamService = teamService;
        this.teamMapper = teamMapper;
        this.userRepository = userRepository;
        this.sessionSecurity = sessionSecurity;
    }

    // ==================== Team CRUD ====================

    /**
     * Create a new team with optional members and leader.
     */
    @PostMapping
    public ResponseEntity<TeamDto> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        logger.info("POST /api/v1/teams - Creating team: {}", request.name());

        UUID creatorId = getCurrentUserId();
        CreateTeamCommand command = CreateTeamCommand.from(request, creatorId);

        TeamCreationResult result = orchestrationService.createTeamWithMembers(command);

        if (result.success()) {
            logger.info("Created team with id: {}", result.team().getId());
            TeamDto dto = teamMapper.toDto(result.team());
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } else {
            logger.warn("Team creation failed: {}", result.errorMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all teams with pagination and filtering.
     */
    @GetMapping
    public ResponseEntity<Page<TeamSummaryDto>> getAllTeams(
            @RequestParam(required = false) TeamStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        logger.info("GET /api/v1/teams - status={}, search={}", status, search);

        Page<TeamSummaryDto> dtos = queryService.findTeams(status, search, pageable);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get team by ID with full details.
     */
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDto> getTeamById(@PathVariable UUID teamId) {
        logger.info("GET /api/v1/teams/{}", teamId);

        return queryService.getTeamDto(teamId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> {
                    logger.warn("Team with id {} not found", teamId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Update team basic information.
     */
    @PutMapping("/{teamId}")
    public ResponseEntity<TeamDto> updateTeam(
            @PathVariable UUID teamId,
            @Valid @RequestBody UpdateTeamRequest request) {
        logger.info("PUT /api/v1/teams/{}", teamId);

        return queryService.updateTeam(teamId, request)
                .map(team -> ResponseEntity.ok(teamMapper.toDto(team)))
                .orElseGet(() -> {
                    logger.warn("Team with id {} not found for update", teamId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Delete (archive) team.
     */
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> archiveTeam(@PathVariable UUID teamId) {
        logger.info("DELETE /api/v1/teams/{} (archive)", teamId);

        ArchiveResult result = orchestrationService.archiveTeam(teamId);
        if (result.success()) {
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Failed to archive team {}: {}", teamId, result.errorMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ==================== Team Lifecycle ====================

    /**
     * Activate team (transition from DRAFT to ACTIVE).
     */
    @PostMapping("/{teamId}/activate")
    public ResponseEntity<ActivationResultDto> activateTeam(@PathVariable UUID teamId) {
        logger.info("POST /api/v1/teams/{}/activate", teamId);

        ActivationResult result = orchestrationService.activateTeam(teamId);
        ActivationResultDto dto = new ActivationResultDto(
                result.success(),
                result.activatedAt(),
                result.errors()
        );

        if (result.success()) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.badRequest().body(dto);
        }
    }

    // ==================== Member Management ====================

    /**
     * Get team members.
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberDto>> getTeamMembers(@PathVariable UUID teamId) {
        logger.info("GET /api/v1/teams/{}/members", teamId);

        List<TeamMemberDto> dtos = queryService.getTeamMembers(teamId);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Add members to team.
     */
    @PostMapping("/{teamId}/members")
    public ResponseEntity<MemberAdditionResultDto> addMembers(
            @PathVariable UUID teamId,
            @Valid @RequestBody AddMembersRequest request) {
        logger.info("POST /api/v1/teams/{}/members - adding {} members", teamId, request.userIds().size());

        String clerkId = sessionSecurity.getAuthenticatedUserId();
        AddMembersCommand command = new AddMembersCommand(request.userIds(), clerkId);
        MemberAdditionResult result = orchestrationService.addMembersToTeam(teamId, command);

        MemberAdditionResultDto dto = new MemberAdditionResultDto(
                result.addedMembers(),
                result.failures(),
                result.hasErrors()
        );

        return ResponseEntity.ok(dto);
    }

    /**
     * Remove member from team.
     */
    @DeleteMapping("/{teamId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID userId) {
        logger.info("DELETE /api/v1/teams/{}/members/{}", teamId, userId);

        boolean removed = queryService.removeMember(teamId, userId);
        if (removed) {
            return ResponseEntity.noContent().build();
        } else {
            logger.warn("Failed to remove member {} from team {}", userId, teamId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Set team leader.
     */
    @PutMapping("/{teamId}/leader")
    public ResponseEntity<LeaderChangeResultDto> setLeader(
            @PathVariable UUID teamId,
            @Valid @RequestBody SetLeaderRequest request) {
        logger.info("PUT /api/v1/teams/{}/leader -> {}", teamId, request.leaderId());

        LeaderChangeResult result = orchestrationService.changeTeamLeader(teamId, request.leaderId());
        LeaderChangeResultDto dto = new LeaderChangeResultDto(
                result.success(),
                result.previousLeaderId(),
                result.newLeaderId()
        );

        if (result.success()) {
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.badRequest().body(dto);
        }
    }

    // ==================== Team Profile & Analytics ====================

    /**
     * Get team competency profile for TEAM_FIT assessments.
     */
    @GetMapping("/{teamId}/profile")
    public ResponseEntity<TeamProfileDto> getTeamProfile(@PathVariable UUID teamId) {
        logger.info("GET /api/v1/teams/{}/profile", teamId);

        Optional<TeamProfile> profileOpt = teamService.getTeamProfile(teamId);
        return profileOpt
                .map(profile -> ResponseEntity.ok(mapTeamProfile(profile)))
                .orElseGet(() -> {
                    logger.warn("Team profile not available for team {}", teamId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Get team skill gaps (undersaturated competencies).
     */
    @GetMapping("/{teamId}/gaps")
    public ResponseEntity<List<UUID>> getSkillGaps(
            @PathVariable UUID teamId,
            @RequestParam(defaultValue = "0.3") double threshold) {
        logger.info("GET /api/v1/teams/{}/gaps?threshold={}", teamId, threshold);

        List<UUID> gaps = teamService.getUndersaturatedCompetencies(teamId, threshold);
        return ResponseEntity.ok(gaps);
    }

    /**
     * Calculate fit score for a candidate against team gaps.
     */
    @PostMapping("/{teamId}/fit-score")
    public ResponseEntity<Map<String, Double>> calculateFitScore(
            @PathVariable UUID teamId,
            @RequestBody Map<UUID, Double> candidateCompetencies) {
        logger.info("POST /api/v1/teams/{}/fit-score", teamId);

        if (!teamService.isValidTeam(teamId)) {
            return ResponseEntity.notFound().build();
        }

        double fitScore = teamService.calculateTeamFitScore(teamId, candidateCompetencies);
        return ResponseEntity.ok(Map.of("fitScore", fitScore));
    }

    // ==================== Current User's Teams ====================

    /**
     * Get teams where current user is a member.
     */
    @GetMapping("/my-teams")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<TeamSummaryDto>> getMyTeams() {
        String clerkId = sessionSecurity.getAuthenticatedUserId();
        logger.info("GET /api/v1/teams/my-teams for user {}", clerkId);

        if (clerkId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<TeamSummaryDto> dtos = queryService.findTeamsByMember(clerkId);
        return ResponseEntity.ok(dtos);
    }

    // ==================== Statistics ====================

    /**
     * Get team statistics.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTeamStats() {
        logger.info("GET /api/v1/teams/stats");

        TeamQueryService.TeamStatistics stats = queryService.getStatistics();
        Map<String, Object> response = new HashMap<>();
        response.put("totalTeams", stats.totalTeams());
        response.put("draftTeams", stats.draftTeams());
        response.put("activeTeams", stats.activeTeams());
        response.put("archivedTeams", stats.archivedTeams());
        return ResponseEntity.ok(response);
    }

    // ==================== Helper Methods ====================

    /**
     * Get the current authenticated user's internal UUID.
     */
    private UUID getCurrentUserId() {
        String clerkId = sessionSecurity.getAuthenticatedUserId();
        if (clerkId == null) {
            throw new IllegalStateException("No authenticated user");
        }

        return userRepository.findByClerkId(clerkId)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "User not found for Clerk ID: " + clerkId));
    }

    private TeamProfileDto mapTeamProfile(TeamProfile profile) {
        // Use competency names collected during aggregation from test result data
        Map<UUID, String> nameMap = profile.competencyNames();

        List<TeamMemberSummaryDto> members = profile.members().stream()
                .map(m -> new TeamMemberSummaryDto(
                        m.userId(),
                        m.name(),
                        m.role()
                ))
                .toList();

        List<CompetencySaturationDto> saturations = profile.competencySaturation().entrySet().stream()
                .map(e -> new CompetencySaturationDto(
                        e.getKey(),
                        nameMap.getOrDefault(e.getKey(), e.getKey().toString().substring(0, 8)),
                        e.getValue()
                ))
                .toList();

        List<SkillGapDto> gaps = profile.skillGaps().stream()
                .map(gapId -> {
                    Double saturation = profile.competencySaturation().getOrDefault(gapId, 0.0);
                    return new SkillGapDto(
                            gapId,
                            nameMap.getOrDefault(gapId, gapId.toString().substring(0, 8)),
                            saturation
                    );
                })
                .toList();

        return new TeamProfileDto(
                profile.teamId(),
                profile.teamName(),
                members,
                saturations,
                profile.averagePersonality(),
                gaps
        );
    }
}
