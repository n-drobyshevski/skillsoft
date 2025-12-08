package app.skillsoft.assessmentbackend.services.external.impl;

import app.skillsoft.assessmentbackend.services.external.TeamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mock implementation of TeamService for development and testing.
 * 
 * In production, this would integrate with team management systems,
 * HR databases, or organizational APIs.
 */
@Service
@Slf4j
public class TeamServiceImpl implements TeamService {

    // In-memory storage for mock team profiles
    private final Map<UUID, TeamProfile> teamStore = new ConcurrentHashMap<>();

    @Override
    public Optional<TeamProfile> getTeamProfile(UUID teamId) {
        log.debug("Fetching team profile for team: {}", teamId);
        return Optional.ofNullable(teamStore.get(teamId));
    }

    @Override
    public Optional<Double> getSaturation(UUID teamId, UUID competencyId) {
        return getTeamProfile(teamId)
            .map(profile -> profile.competencySaturation().get(competencyId));
    }

    @Override
    public List<UUID> getUndersaturatedCompetencies(UUID teamId, double threshold) {
        return getTeamProfile(teamId)
            .map(profile -> profile.competencySaturation().entrySet().stream()
                .filter(entry -> entry.getValue() < threshold)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()))
            .orElse(List.of());
    }

    @Override
    public double calculateTeamFitScore(UUID teamId, Map<UUID, Double> candidateCompetencies) {
        return getTeamProfile(teamId)
            .map(profile -> {
                // Calculate fit score based on how well candidate fills gaps
                var gaps = profile.skillGaps();
                if (gaps.isEmpty()) {
                    return 50.0; // Neutral score if no gaps
                }

                double totalGapFill = 0.0;
                int matchingGaps = 0;

                for (UUID gapCompetencyId : gaps) {
                    Double candidateScore = candidateCompetencies.get(gapCompetencyId);
                    if (candidateScore != null && candidateScore >= 3.0) {
                        // Candidate has decent proficiency in this gap area
                        totalGapFill += (candidateScore / 5.0) * 100;
                        matchingGaps++;
                    }
                }

                if (matchingGaps == 0) {
                    return 25.0; // Low score if candidate doesn't fill any gaps
                }

                // Weight by percentage of gaps filled
                double gapCoverage = (double) matchingGaps / gaps.size();
                double avgGapFill = totalGapFill / matchingGaps;

                return (avgGapFill * 0.6) + (gapCoverage * 40);
            })
            .orElse(0.0);
    }

    @Override
    public boolean isValidTeam(UUID teamId) {
        return teamStore.containsKey(teamId);
    }

    /**
     * Create a demo team profile for testing purposes.
     * 
     * @param teamId The team ID
     * @param teamName The team name
     * @param members List of team member profiles
     * @return The created team profile
     */
    public TeamProfile createDemoTeamProfile(UUID teamId, String teamName, List<TeamMemberProfile> members) {
        // Calculate saturation from member competencies
        Map<UUID, List<Double>> competencyScoresMap = new HashMap<>();
        Map<String, List<Double>> personalityScoresMap = new HashMap<>();

        for (TeamMemberProfile member : members) {
            // Aggregate competency scores
            member.competencyScores().forEach((compId, score) -> 
                competencyScoresMap.computeIfAbsent(compId, k -> new ArrayList<>()).add(score)
            );
            
            // Aggregate personality traits
            if (member.personalityTraits() != null) {
                member.personalityTraits().forEach((trait, score) ->
                    personalityScoresMap.computeIfAbsent(trait, k -> new ArrayList<>()).add(score)
                );
            }
        }

        // Calculate saturation (normalize to 0-1 based on team size and scores)
        Map<UUID, Double> saturation = new HashMap<>();
        int teamSize = members.size();
        competencyScoresMap.forEach((compId, scores) -> {
            double avgScore = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double coverage = (double) scores.size() / teamSize;
            // Saturation = coverage * normalized score
            saturation.put(compId, coverage * (avgScore / 5.0));
        });

        // Calculate average personality
        Map<String, Double> avgPersonality = new HashMap<>();
        personalityScoresMap.forEach((trait, scores) ->
            avgPersonality.put(trait, scores.stream().mapToDouble(Double::doubleValue).average().orElse(0))
        );

        // Identify skill gaps (saturation < 0.3)
        List<UUID> skillGaps = saturation.entrySet().stream()
            .filter(entry -> entry.getValue() < 0.3)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());

        TeamProfile profile = new TeamProfile(
            teamId,
            teamName,
            members,
            saturation,
            avgPersonality,
            skillGaps
        );

        teamStore.put(teamId, profile);
        log.debug("Created demo team profile for team: {} with {} members", teamName, members.size());
        return profile;
    }

    /**
     * Helper to create a team member profile.
     */
    public TeamMemberProfile createMemberProfile(
            UUID userId, 
            String name, 
            String role,
            Map<UUID, Double> competencyScores,
            Map<String, Double> personalityTraits) {
        return new TeamMemberProfile(userId, name, role, competencyScores, personalityTraits);
    }
}