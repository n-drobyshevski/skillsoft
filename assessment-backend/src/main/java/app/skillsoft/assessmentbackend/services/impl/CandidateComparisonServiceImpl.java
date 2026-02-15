package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.CompetencyScoreDto;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TeamFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.comparison.*;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.services.CandidateComparisonService;
import app.skillsoft.assessmentbackend.services.external.TeamService;
import app.skillsoft.assessmentbackend.services.external.TeamService.TeamProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of candidate comparison service for Team Fit assessments.
 *
 * Compares 2-5 candidate results on the same TEAM_FIT template, producing:
 * - Ranked candidate summaries across multiple dimensions
 * - Per-competency comparison with team gap identification
 * - Gap coverage matrix showing which candidates fill team gaps
 * - Pairwise complementarity scores for candidate pairs
 */
@Service
@Transactional(readOnly = true)
public class CandidateComparisonServiceImpl implements CandidateComparisonService {

    private static final Logger log = LoggerFactory.getLogger(CandidateComparisonServiceImpl.class);

    private static final int MIN_CANDIDATES = 2;
    private static final int MAX_CANDIDATES = 5;
    private static final double DEFAULT_DIVERSITY_THRESHOLD = 0.5;
    private static final double GAP_COVERAGE_THRESHOLD = 50.0;

    private final TestResultRepository testResultRepository;
    private final TeamService teamService;

    public CandidateComparisonServiceImpl(TestResultRepository testResultRepository,
                                          TeamService teamService) {
        this.testResultRepository = testResultRepository;
        this.teamService = teamService;
    }

    @Override
    public CandidateComparisonDto compareResults(List<UUID> resultIds, UUID templateId) {
        // 1. Validate input size
        validateResultCount(resultIds);

        // 2. Fetch all results with session and template eagerly loaded
        List<TestResult> results = testResultRepository.findAllByIdInWithSessionAndTemplate(resultIds);

        // 3. Validate all results exist
        if (results.size() != resultIds.size()) {
            Set<UUID> foundIds = results.stream().map(TestResult::getId).collect(Collectors.toSet());
            List<UUID> missing = resultIds.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new IllegalArgumentException("Results not found: " + missing);
        }

        // 4. Validate all results belong to the given template
        for (TestResult result : results) {
            UUID resultTemplateId = result.getSession().getTemplate().getId();
            if (!resultTemplateId.equals(templateId)) {
                throw new IllegalArgumentException(
                    "Result " + result.getId() + " belongs to template " + resultTemplateId
                    + ", expected " + templateId);
            }
        }

        // 5. Validate template goal is TEAM_FIT
        TestTemplate template = results.get(0).getSession().getTemplate();
        if (template.getGoal() != AssessmentGoal.TEAM_FIT) {
            throw new IllegalArgumentException(
                "Comparison is only supported for TEAM_FIT templates, got: " + template.getGoal());
        }

        // 6. Extract team context from blueprint
        TeamFitBlueprint blueprint = extractTeamFitBlueprint(template);
        UUID teamId = blueprint != null ? blueprint.getTeamId() : null;
        String targetRole = blueprint != null ? blueprint.getTargetRole() : null;
        double diversityThreshold = DEFAULT_DIVERSITY_THRESHOLD;

        // 7. Fetch team profile (graceful fallback if unavailable)
        TeamProfile teamProfile = null;
        if (teamId != null) {
            teamProfile = teamService.getTeamProfile(teamId).orElse(null);
            if (teamProfile == null) {
                log.info("Team {} not found or inactive — comparison will proceed without team context", teamId);
            }
        }

        boolean teamAvailable = teamProfile != null;
        int teamSize = teamAvailable ? teamProfile.members().size() : 0;
        Map<UUID, Double> teamCompetencySaturationById = teamAvailable
            ? teamProfile.competencySaturation()
            : Collections.emptyMap();

        // Convert UUID keys to string keys for the response DTO
        Map<String, Double> teamCompetencySaturation = teamCompetencySaturationById.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));

        // 8. Build candidate summaries (unranked initially)
        List<CandidateSummaryHolder> holders = results.stream()
            .map(this::buildCandidateHolder)
            .toList();

        // 9. Assign ranks across multiple dimensions
        List<CandidateSummaryDto> candidates = assignRanks(holders);

        // 10. Build competency comparison
        List<CompetencyComparisonDto> competencyComparison = buildCompetencyComparison(
            results, teamCompetencySaturationById, diversityThreshold);

        // 11. Build gap coverage matrix
        List<GapCoverageEntryDto> gapCoverageMatrix = buildGapCoverageMatrix(competencyComparison, results);

        // 12. Build complementarity pairs
        List<CandidatePairComplementarityDto> complementarityPairs = buildComplementarityPairs(
            results, gapCoverageMatrix, holders);

        log.info("Comparison complete: {} candidates, {} competencies, {} gaps, {} pairs",
            candidates.size(), competencyComparison.size(),
            gapCoverageMatrix.size(), complementarityPairs.size());

        return new CandidateComparisonDto(
            templateId,
            template.getName(),
            teamId,
            targetRole,
            teamSize,
            teamAvailable,
            candidates,
            competencyComparison,
            gapCoverageMatrix,
            complementarityPairs,
            teamCompetencySaturation
        );
    }

    // ============================================
    // VALIDATION
    // ============================================

    private void validateResultCount(List<UUID> resultIds) {
        if (resultIds == null || resultIds.size() < MIN_CANDIDATES) {
            throw new IllegalArgumentException(
                "At least " + MIN_CANDIDATES + " results are required for comparison");
        }
        if (resultIds.size() > MAX_CANDIDATES) {
            throw new IllegalArgumentException(
                "At most " + MAX_CANDIDATES + " results can be compared at once");
        }
    }

    // ============================================
    // BLUEPRINT EXTRACTION
    // ============================================

    /**
     * Extract TeamFitBlueprint from template configuration.
     * Replicates the extraction logic from TeamFitScoringStrategy for decoupling.
     */
    private TeamFitBlueprint extractTeamFitBlueprint(TestTemplate template) {
        if (template == null) {
            return null;
        }

        TestBlueprintDto typedBlueprint = template.getTypedBlueprint();
        if (typedBlueprint instanceof TeamFitBlueprint teamFit) {
            return teamFit;
        }

        // Fallback: create from legacy blueprint if available
        if (template.getBlueprint() != null) {
            Map<String, Object> legacyBlueprint = template.getBlueprint();
            TeamFitBlueprint teamFitBlueprint = new TeamFitBlueprint();

            Object teamIdObj = legacyBlueprint.get("teamId");
            if (teamIdObj != null) {
                try {
                    teamFitBlueprint.setTeamId(UUID.fromString(teamIdObj.toString()));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid team ID format in blueprint: {}", teamIdObj);
                }
            }

            Object threshold = legacyBlueprint.get("saturationThreshold");
            if (threshold instanceof Number number) {
                teamFitBlueprint.setSaturationThreshold(number.doubleValue());
            }

            Object targetRoleObj = legacyBlueprint.get("targetRole");
            if (targetRoleObj instanceof String role) {
                teamFitBlueprint.setTargetRole(role);
            }

            return teamFitBlueprint;
        }

        return null;
    }

    // ============================================
    // CANDIDATE SUMMARY BUILDING
    // ============================================

    /**
     * Intermediate holder for candidate data before ranking is applied.
     */
    private record CandidateSummaryHolder(
        UUID resultId,
        String displayName,
        Double overallPercentage,
        Boolean passed,
        Double diversityRatio,
        Double saturationRatio,
        Double teamFitMultiplier,
        Double personalityCompatibility,
        Map<String, Double> bigFiveProfile,
        Map<String, Double> competencySaturation,
        String completedAt
    ) {}

    private CandidateSummaryHolder buildCandidateHolder(TestResult result) {
        String displayName = result.getDisplayName();

        Map<String, Double> bigFiveProfile = result.getBigFiveProfile() != null
            ? result.getBigFiveProfile()
            : Collections.emptyMap();

        Map<String, Object> extended = result.getExtendedMetrics() != null
            ? result.getExtendedMetrics()
            : Collections.emptyMap();

        Double diversityRatio = extractDouble(extended, "diversityRatio");
        Double saturationRatio = extractDouble(extended, "saturationRatio");
        Double teamFitMultiplier = extractDouble(extended, "teamFitMultiplier");
        Double personalityCompatibility = extractDouble(extended, "personalityCompatibility");
        Map<String, Double> competencySaturation = extractDoubleMap(extended, "competencySaturation");

        String completedAt = result.getCompletedAt() != null
            ? result.getCompletedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            : null;

        return new CandidateSummaryHolder(
            result.getId(),
            displayName,
            result.getOverallPercentage(),
            result.getPassed(),
            diversityRatio,
            saturationRatio,
            teamFitMultiplier,
            personalityCompatibility,
            bigFiveProfile,
            competencySaturation,
            completedAt
        );
    }

    // ============================================
    // RANKING
    // ============================================

    private List<CandidateSummaryDto> assignRanks(List<CandidateSummaryHolder> holders) {
        // Sort by overallPercentage descending for overallRank
        List<CandidateSummaryHolder> byOverall = new ArrayList<>(holders);
        byOverall.sort(Comparator.comparing(
            CandidateSummaryHolder::overallPercentage,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        Map<UUID, Integer> overallRanks = buildRankMap(byOverall, CandidateSummaryHolder::resultId);

        // Sort by diversityRatio descending for diversityRank
        List<CandidateSummaryHolder> byDiversity = new ArrayList<>(holders);
        byDiversity.sort(Comparator.comparing(
            CandidateSummaryHolder::diversityRatio,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        Map<UUID, Integer> diversityRanks = buildRankMap(byDiversity, CandidateSummaryHolder::resultId);

        // Sort by personalityCompatibility descending for personalityRank
        List<CandidateSummaryHolder> byPersonality = new ArrayList<>(holders);
        byPersonality.sort(Comparator.comparing(
            CandidateSummaryHolder::personalityCompatibility,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));
        Map<UUID, Integer> personalityRanks = buildRankMap(byPersonality, CandidateSummaryHolder::resultId);

        return holders.stream()
            .map(h -> new CandidateSummaryDto(
                h.resultId(),
                h.displayName(),
                h.overallPercentage(),
                h.passed(),
                overallRanks.getOrDefault(h.resultId(), holders.size()),
                diversityRanks.getOrDefault(h.resultId(), holders.size()),
                personalityRanks.getOrDefault(h.resultId(), holders.size()),
                h.diversityRatio(),
                h.saturationRatio(),
                h.teamFitMultiplier(),
                h.personalityCompatibility(),
                h.bigFiveProfile(),
                h.competencySaturation(),
                h.completedAt()
            ))
            .toList();
    }

    private <T> Map<UUID, Integer> buildRankMap(List<T> sorted,
                                                 java.util.function.Function<T, UUID> idExtractor) {
        Map<UUID, Integer> ranks = new LinkedHashMap<>();
        for (int i = 0; i < sorted.size(); i++) {
            ranks.put(idExtractor.apply(sorted.get(i)), i + 1);
        }
        return ranks;
    }

    // ============================================
    // COMPETENCY COMPARISON
    // ============================================

    private List<CompetencyComparisonDto> buildCompetencyComparison(
            List<TestResult> results,
            Map<UUID, Double> teamSaturationMap,
            double diversityThreshold) {

        // Collect all competency scores across all candidates
        // Key: competencyId, Value: map of resultId -> percentage
        Map<UUID, String> competencyNames = new LinkedHashMap<>();
        Map<UUID, Map<UUID, Double>> competencyScoresMap = new LinkedHashMap<>();

        for (TestResult result : results) {
            List<CompetencyScoreDto> scores = result.getCompetencyScores();
            if (scores == null) continue;

            for (CompetencyScoreDto score : scores) {
                UUID compId = score.getCompetencyId();
                competencyNames.putIfAbsent(compId, score.getCompetencyName());
                competencyScoresMap
                    .computeIfAbsent(compId, k -> new LinkedHashMap<>())
                    .put(result.getId(), score.getPercentage() != null ? score.getPercentage() : 0.0);
            }
        }

        List<CompetencyComparisonDto> comparisons = new ArrayList<>();
        for (Map.Entry<UUID, Map<UUID, Double>> entry : competencyScoresMap.entrySet()) {
            UUID compId = entry.getKey();
            Map<UUID, Double> candidateScores = entry.getValue();
            String compName = competencyNames.get(compId);

            Double teamSaturation = teamSaturationMap.getOrDefault(compId, null);
            boolean isTeamGap = teamSaturation != null && teamSaturation < diversityThreshold;

            // Determine best candidate for this competency
            UUID bestCandidateId = candidateScores.entrySet().stream()
                .max(Comparator.comparingDouble(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

            comparisons.add(new CompetencyComparisonDto(
                compId, compName, teamSaturation, candidateScores, bestCandidateId, isTeamGap
            ));
        }

        return comparisons;
    }

    // ============================================
    // GAP COVERAGE MATRIX
    // ============================================

    private List<GapCoverageEntryDto> buildGapCoverageMatrix(
            List<CompetencyComparisonDto> competencyComparisons,
            List<TestResult> results) {

        return competencyComparisons.stream()
            .filter(CompetencyComparisonDto::isTeamGap)
            .map(comp -> {
                // Filter candidates scoring above the gap coverage threshold
                Map<UUID, Double> candidateCoverage = comp.candidateScores().entrySet().stream()
                    .filter(e -> e.getValue() >= GAP_COVERAGE_THRESHOLD)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));

                UUID bestCandidateId = candidateCoverage.entrySet().stream()
                    .max(Comparator.comparingDouble(Map.Entry::getValue))
                    .map(Map.Entry::getKey)
                    .orElse(null);

                return new GapCoverageEntryDto(
                    comp.competencyId(),
                    comp.competencyName(),
                    comp.teamSaturation(),
                    candidateCoverage,
                    bestCandidateId
                );
            })
            .toList();
    }

    // ============================================
    // COMPLEMENTARITY PAIRS
    // ============================================

    private List<CandidatePairComplementarityDto> buildComplementarityPairs(
            List<TestResult> results,
            List<GapCoverageEntryDto> gapCoverageMatrix,
            List<CandidateSummaryHolder> holders) {

        int totalGaps = gapCoverageMatrix.size();
        if (totalGaps == 0) {
            return Collections.emptyList();
        }

        // Build name lookup
        Map<UUID, String> nameMap = holders.stream()
            .collect(Collectors.toMap(CandidateSummaryHolder::resultId, CandidateSummaryHolder::displayName));

        // Build per-candidate gap coverage sets
        // candidateId -> set of competencyIds they cover (score >= threshold)
        Map<UUID, Set<UUID>> candidateGapCoverage = new HashMap<>();
        for (TestResult result : results) {
            candidateGapCoverage.put(result.getId(), new HashSet<>());
        }
        for (GapCoverageEntryDto gap : gapCoverageMatrix) {
            for (UUID candidateId : gap.candidateCoverage().keySet()) {
                candidateGapCoverage.computeIfAbsent(candidateId, k -> new HashSet<>())
                    .add(gap.competencyId());
            }
        }

        // Generate all unique pairs
        List<UUID> resultIdList = results.stream().map(TestResult::getId).toList();
        List<CandidatePairComplementarityDto> pairs = new ArrayList<>();

        for (int i = 0; i < resultIdList.size(); i++) {
            for (int j = i + 1; j < resultIdList.size(); j++) {
                UUID candidateA = resultIdList.get(i);
                UUID candidateB = resultIdList.get(j);

                Set<UUID> combinedCoverage = new HashSet<>();
                combinedCoverage.addAll(candidateGapCoverage.getOrDefault(candidateA, Collections.emptySet()));
                combinedCoverage.addAll(candidateGapCoverage.getOrDefault(candidateB, Collections.emptySet()));

                int combinedGapsCovered = combinedCoverage.size();
                double complementarityScore = (double) combinedGapsCovered / totalGaps * 100.0;

                pairs.add(new CandidatePairComplementarityDto(
                    candidateA,
                    candidateB,
                    nameMap.getOrDefault(candidateA, candidateA.toString()),
                    nameMap.getOrDefault(candidateB, candidateB.toString()),
                    complementarityScore,
                    combinedGapsCovered,
                    totalGaps
                ));
            }
        }

        return pairs;
    }

    // ============================================
    // UTILITY METHODS
    // ============================================

    private Double extractDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> extractDoubleMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, Double> result = new LinkedHashMap<>();
            rawMap.forEach((k, v) -> {
                if (v instanceof Number number) {
                    result.put(k.toString(), number.doubleValue());
                }
            });
            return result;
        }
        return Collections.emptyMap();
    }
}
