package app.skillsoft.assessmentbackend.services.team;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TeamMember;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.repository.TeamMemberRepository;
import app.skillsoft.assessmentbackend.repository.TeamRepository;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamMemberProfile;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for computing team profiles from TestResult data.
 * Aggregates member competency scores and calculates saturation levels.
 */
@Service
@Transactional(readOnly = true)
public class TeamProfileAggregationService {

    private static final Logger log = LoggerFactory.getLogger(TeamProfileAggregationService.class);
    private static final double DEFAULT_SATURATION_THRESHOLD = 0.3;

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TestResultRepository testResultRepository;

    public TeamProfileAggregationService(
            TeamRepository teamRepository,
            TeamMemberRepository teamMemberRepository,
            TestResultRepository testResultRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.testResultRepository = testResultRepository;
    }

    /**
     * Compute the full team profile with aggregated competency data.
     */
    public Optional<TeamProfile> computeTeamProfile(UUID teamId) {
        Optional<Team> teamOpt = teamRepository.findById(teamId);

        if (teamOpt.isEmpty()) {
            log.warn("Team not found: {}", teamId);
            return Optional.empty();
        }

        Team team = teamOpt.get();

        // Only compute profiles for ACTIVE teams
        if (team.getStatus() != TeamStatus.ACTIVE) {
            log.debug("Team {} is not active (status={}), returning empty profile", teamId, team.getStatus());
            return Optional.empty();
        }

        List<TeamMember> activeMembers = teamMemberRepository.findByTeamIdAndIsActiveTrue(teamId);

        if (activeMembers.isEmpty()) {
            log.warn("Team {} has no active members", teamId);
            return Optional.of(new TeamProfile(
                    teamId, team.getName(), List.of(),
                    Map.of(), Map.of(), List.of()
            ));
        }

        // Build member profiles from test results
        List<TeamMemberProfile> memberProfiles = buildMemberProfiles(activeMembers);

        // Calculate competency saturation
        Map<UUID, Double> competencySaturation = calculateCompetencySaturation(
                memberProfiles, activeMembers.size()
        );

        // Calculate average personality traits
        Map<String, Double> averagePersonality = calculateAveragePersonality(memberProfiles);

        // Identify skill gaps (saturation < threshold)
        List<UUID> skillGaps = competencySaturation.entrySet().stream()
                .filter(e -> e.getValue() < DEFAULT_SATURATION_THRESHOLD)
                .map(Map.Entry::getKey)
                .toList();

        return Optional.of(new TeamProfile(
                teamId,
                team.getName(),
                memberProfiles,
                competencySaturation,
                averagePersonality,
                skillGaps
        ));
    }

    private List<TeamMemberProfile> buildMemberProfiles(List<TeamMember> members) {
        return members.stream()
                .map(this::buildMemberProfile)
                .filter(Objects::nonNull)
                .toList();
    }

    private TeamMemberProfile buildMemberProfile(TeamMember member) {
        User user = member.getUser();
        String clerkUserId = user.getClerkId();

        // Get latest test results for competency scores
        var results = testResultRepository.findByClerkUserIdOrderByCompletedAtDesc(clerkUserId);

        if (results.isEmpty()) {
            // Return profile with empty scores
            return new TeamMemberProfile(
                    user.getId(),
                    user.getFullName(),
                    member.getRole().getDisplayName(),
                    Map.of(),
                    Map.of()
            );
        }

        // Aggregate competency scores from all test results
        Map<UUID, Double> competencyScores = aggregateCompetencyScores(results);

        // Get personality traits from results with Big Five profile
        Map<String, Double> personalityTraits = extractPersonalityTraits(results);

        return new TeamMemberProfile(
                user.getId(),
                user.getFullName(),
                member.getRole().getDisplayName(),
                competencyScores,
                personalityTraits
        );
    }

    private Map<UUID, Double> aggregateCompetencyScores(List<TestResult> results) {
        Map<UUID, List<Double>> scoresByCompetency = new HashMap<>();

        for (TestResult result : results) {
            List<CompetencyScoreDto> scores = result.getCompetencyScores();
            if (scores != null) {
                for (CompetencyScoreDto score : scores) {
                    UUID compId = score.getCompetencyId();
                    Double percentage = score.getPercentage();
                    if (compId != null && percentage != null) {
                        scoresByCompetency
                                .computeIfAbsent(compId, k -> new ArrayList<>())
                                .add(percentage);
                    }
                }
            }
        }

        // Use latest score for each competency (first in list since ordered by date desc)
        return scoresByCompetency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().isEmpty() ? 0.0 : e.getValue().get(0) / 100.0 * 5.0 // Convert percentage to 1-5 scale
                ));
    }

    private Map<String, Double> extractPersonalityTraits(List<TestResult> results) {
        // Find first result with Big Five profile
        for (TestResult result : results) {
            Map<String, Double> profile = result.getBigFiveProfile();
            if (profile != null && !profile.isEmpty()) {
                return profile;
            }
        }
        return Map.of();
    }

    private Map<UUID, Double> calculateCompetencySaturation(
            List<TeamMemberProfile> members, int teamSize) {

        Map<UUID, List<Double>> scoresByCompetency = new HashMap<>();

        for (TeamMemberProfile member : members) {
            if (member.competencyScores() != null) {
                member.competencyScores().forEach((compId, score) ->
                        scoresByCompetency
                                .computeIfAbsent(compId, k -> new ArrayList<>())
                                .add(score)
                );
            }
        }

        // Saturation = (count of members with skill / team size) * (avg score / max score)
        return scoresByCompetency.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            double coverage = (double) e.getValue().size() / teamSize;
                            double avgScore = e.getValue().stream()
                                    .mapToDouble(Double::doubleValue)
                                    .average()
                                    .orElse(0);
                            return coverage * (avgScore / 5.0);
                        }
                ));
    }

    private Map<String, Double> calculateAveragePersonality(List<TeamMemberProfile> members) {
        Map<String, List<Double>> traitScores = new HashMap<>();

        for (TeamMemberProfile member : members) {
            if (member.personalityTraits() != null) {
                member.personalityTraits().forEach((trait, score) ->
                        traitScores.computeIfAbsent(trait, k -> new ArrayList<>()).add(score)
                );
            }
        }

        return traitScores.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)
                ));
    }
}
