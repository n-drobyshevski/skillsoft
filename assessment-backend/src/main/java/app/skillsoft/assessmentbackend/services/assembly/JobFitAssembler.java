package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.external.PassportService;
import app.skillsoft.assessmentbackend.services.selection.QuestionSelectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Assembler for JOB_FIT (Targeted Fit) assessment strategy.
 *
 * Algorithm:
 * 1. Fetch O*NET Benchmark for the specified SOC code
 * 2. If candidate has a Competency Passport, calculate gaps (Benchmark - PassportScore)
 * 3. For gaps > 0.2 (significant deficit), select ADVANCED difficulty questions
 * 4. For smaller gaps, select INTERMEDIATE difficulty questions
 * 5. Apply strictness level to adjust question difficulty selection
 *
 * This implements Delta Testing - only assessing areas where the candidate needs evaluation.
 *
 * Refactored to use QuestionSelectionService for centralized question selection logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobFitAssembler implements TestAssembler {

    private final OnetService onetService;
    private final PassportService passportService;
    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final QuestionSelectionService questionSelectionService;

    /**
     * Gap threshold for selecting ADVANCED questions.
     */
    private static final double SIGNIFICANT_GAP_THRESHOLD = 0.2;

    /**
     * Default questions per competency gap area.
     */
    private static final int DEFAULT_QUESTIONS_PER_GAP = 5;

    @Override
    public AssessmentGoal getSupportedGoal() {
        return AssessmentGoal.JOB_FIT;
    }

    @Override
    public List<UUID> assemble(TestBlueprintDto blueprint) {
        if (!(blueprint instanceof JobFitBlueprint jobFitBlueprint)) {
            throw new IllegalArgumentException(
                "JobFitAssembler requires JobFitBlueprint, got: " +
                (blueprint != null ? blueprint.getClass().getSimpleName() : "null")
            );
        }

        var socCode = jobFitBlueprint.getOnetSocCode();
        if (socCode == null || socCode.isBlank()) {
            log.warn("No O*NET SOC code provided in JobFitBlueprint");
            return List.of();
        }

        var candidateClerkUserId = jobFitBlueprint.getCandidateClerkUserId();
        log.info("Assembling JOB_FIT test for SOC code: {}, candidateClerkUserId: {}",
            socCode, candidateClerkUserId != null ? candidateClerkUserId : "(none - full assessment)");

        // Step 1: Fetch O*NET profile/benchmarks
        var onetProfile = onetService.getProfile(socCode);
        if (onetProfile.isEmpty()) {
            log.warn("No O*NET profile found for SOC code: {}", socCode);
            return List.of();
        }

        var benchmarks = onetProfile.get().benchmarks();
        log.debug("Found {} benchmark competencies for {}", benchmarks.size(), socCode);

        // Step 2: Fetch candidate's Competency Passport if available (Delta Testing)
        Optional<PassportService.CompetencyPassport> passport = fetchCandidatePassport(candidateClerkUserId);

        // Step 3: Calculate gaps using passport data (or default to score=0 if no passport)
        var gapAnalysis = analyzeGaps(benchmarks, passport, jobFitBlueprint.getStrictnessLevel());

        // Step 4: Select questions based on gap analysis using QuestionSelectionService
        var selectedQuestions = selectQuestionsForGaps(gapAnalysis, jobFitBlueprint.getStrictnessLevel());

        log.info("Assembled {} questions for JOB_FIT assessment (SOC: {}, deltaMode: {})",
            selectedQuestions.size(), socCode, passport.isPresent());

        return selectedQuestions;
    }

    /**
     * Fetch the candidate's Competency Passport from PassportService.
     *
     * @param candidateClerkUserId The Clerk User ID of the candidate (may be null)
     * @return The passport if found and valid, empty otherwise
     */
    private Optional<PassportService.CompetencyPassport> fetchCandidatePassport(String candidateClerkUserId) {
        if (candidateClerkUserId == null || candidateClerkUserId.isBlank()) {
            log.debug("No candidate ID provided - using full assessment mode (all scores = 0)");
            return Optional.empty();
        }

        var passport = passportService.getPassportByClerkUserId(candidateClerkUserId);

        if (passport.isPresent()) {
            var p = passport.get();
            log.info("Found Competency Passport for candidate {}: {} competency scores, last assessed: {}",
                candidateClerkUserId, p.competencyScores().size(), p.lastAssessed());
        } else {
            log.debug("No valid Competency Passport found for candidate {} - using full assessment mode",
                candidateClerkUserId);
        }

        return passport;
    }

    /**
     * Analyze gaps between O*NET benchmarks and candidate's passport scores.
     *
     * Gap Calculation:
     * - gap = benchmark - candidateScore
     * - Positive gap = candidate needs improvement
     * - Zero/negative gap = candidate meets or exceeds benchmark
     *
     * For candidates without a passport, all scores default to 0.0,
     * resulting in gap = benchmark (full assessment mode).
     *
     * @param benchmarks Map of competency names to benchmark scores (from O*NET)
     * @param passport The candidate's Competency Passport (may be empty)
     * @param strictnessLevel Strictness level (0-100) affecting gap threshold
     * @return Map of competency names to gap analysis info
     */
    private Map<String, GapInfo> analyzeGaps(
            Map<String, Double> benchmarks,
            Optional<PassportService.CompetencyPassport> passport,
            int strictnessLevel) {

        var gaps = new HashMap<String, GapInfo>();

        // Adjust threshold based on strictness level
        // Higher strictness = lower threshold = more gaps considered significant
        var adjustedThreshold = SIGNIFICANT_GAP_THRESHOLD * (100.0 - strictnessLevel) / 100.0;

        // Extract passport scores if available
        Map<UUID, Double> passportScores = passport
            .map(PassportService.CompetencyPassport::competencyScores)
            .orElse(Map.of());

        // Build a lookup map from competency name to passport score
        // This requires mapping O*NET competency names to internal competency IDs
        Map<String, Double> passportScoresByName = buildPassportScoreLookup(passportScores);

        for (var entry : benchmarks.entrySet()) {
            var competencyName = entry.getKey();
            var benchmark = entry.getValue();

            // Look up candidate's existing score for this competency
            // Default to 0.0 if no passport or competency not assessed
            var candidateScore = passportScoresByName.getOrDefault(competencyName, 0.0);

            // Calculate gap: how much the candidate falls short of the benchmark
            var gap = Math.max(0.0, benchmark - candidateScore);

            // Determine if this gap is significant enough to warrant advanced questions
            var isSignificant = gap > adjustedThreshold;

            gaps.put(competencyName, new GapInfo(
                competencyName,
                benchmark,
                candidateScore,
                gap,
                isSignificant
            ));

            log.debug("Gap analysis for '{}': benchmark={}, candidateScore={}, gap={}, significant={}",
                competencyName, benchmark, candidateScore, gap, isSignificant);
        }

        // Log summary
        long significantGaps = gaps.values().stream().filter(GapInfo::isSignificant).count();
        long metBenchmarks = gaps.values().stream().filter(g -> g.gap() <= 0).count();
        log.info("Gap analysis summary: {} competencies analyzed, {} significant gaps, {} benchmarks met",
            gaps.size(), significantGaps, metBenchmarks);

        return gaps;
    }

    /**
     * Build a lookup map from competency names to passport scores.
     *
     * This method maps internal competency UUIDs to their names so we can
     * match them against O*NET benchmark competency names.
     *
     * @param passportScores Map of competency UUIDs to scores from passport
     * @return Map of competency names to scores
     */
    private Map<String, Double> buildPassportScoreLookup(Map<UUID, Double> passportScores) {
        if (passportScores.isEmpty()) {
            return Map.of();
        }

        var lookup = new HashMap<String, Double>();

        for (var entry : passportScores.entrySet()) {
            var competencyId = entry.getKey();
            var score = entry.getValue();

            // Look up the competency name from the repository
            competencyRepository.findById(competencyId).ifPresent(competency -> {
                // Use the competency name for matching
                lookup.put(competency.getName(), score);

                // Also check standard_codes for O*NET code mapping if available
                var standardCodes = competency.getStandardCodes();
                if (standardCodes != null && standardCodes.hasOnetMapping()) {
                    var onetCode = standardCodes.onetRef().code();
                    if (onetCode != null && !onetCode.isBlank()) {
                        lookup.put(onetCode, score);
                    }
                    // Also add by O*NET title if available
                    var onetTitle = standardCodes.onetRef().title();
                    if (onetTitle != null && !onetTitle.isBlank()) {
                        lookup.put(onetTitle, score);
                    }
                }
            });
        }

        log.debug("Built passport score lookup with {} entries", lookup.size());
        return lookup;
    }

    /**
     * Select questions based on gap analysis using QuestionSelectionService.
     *
     * Uses WEIGHTED distribution with gap magnitude as weight.
     * Larger gaps receive more questions.
     */
    private List<UUID> selectQuestionsForGaps(Map<String, GapInfo> gapAnalysis, int strictnessLevel) {
        // Build weighted indicator map based on gaps
        Map<UUID, Double> indicatorWeights = new LinkedHashMap<>();
        Map<UUID, DifficultyLevel> indicatorDifficulties = new HashMap<>();

        // Sort gaps by significance (largest gaps first)
        var sortedGaps = gapAnalysis.values().stream()
            .sorted(Comparator.comparing(GapInfo::gap).reversed())
            .toList();

        for (var gapInfo : sortedGaps) {
            // Determine target difficulty based on gap significance
            var targetDifficulty = gapInfo.isSignificant()
                ? DifficultyLevel.ADVANCED
                : DifficultyLevel.INTERMEDIATE;

            // Find competency and its indicators by name
            var indicatorsForGap = findIndicatorsForCompetencyName(gapInfo.competencyName());

            for (var indicator : indicatorsForGap) {
                // Weight = gap magnitude (0.0 - 1.0), minimum 0.1 to ensure at least some questions
                double weight = Math.max(0.1, gapInfo.gap());
                indicatorWeights.put(indicator.getId(), weight);
                indicatorDifficulties.put(indicator.getId(), targetDifficulty);
            }
        }

        if (indicatorWeights.isEmpty()) {
            log.warn("No indicators found for gap analysis competencies");
            return List.of();
        }

        // Calculate total questions based on gap count
        int totalQuestions = Math.min(
            gapAnalysis.size() * DEFAULT_QUESTIONS_PER_GAP,
            50  // Cap at 50 questions for reasonable test length
        );

        // Select questions with weighted distribution
        // Questions for higher-gap competencies get priority
        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();

        // Sort indicators by weight (highest gaps first)
        List<UUID> sortedIndicators = indicatorWeights.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

        for (UUID indicatorId : sortedIndicators) {
            if (selectedQuestions.size() >= totalQuestions) break;

            DifficultyLevel difficulty = indicatorDifficulties.get(indicatorId);
            double weight = indicatorWeights.get(indicatorId);

            // Questions per indicator proportional to weight
            int questionsForIndicator = Math.max(1, (int) Math.round(weight * DEFAULT_QUESTIONS_PER_GAP));
            questionsForIndicator = Math.min(questionsForIndicator, totalQuestions - selectedQuestions.size());

            List<UUID> questions = questionSelectionService.selectQuestionsForIndicator(
                indicatorId,
                questionsForIndicator,
                difficulty,
                usedQuestions
            );

            selectedQuestions.addAll(questions);
            usedQuestions.addAll(questions);
        }

        log.debug("Selected {} questions for {} gaps using weighted distribution",
            selectedQuestions.size(), gapAnalysis.size());

        return selectedQuestions;
    }

    /**
     * Find behavioral indicators matching a competency name.
     * Matches by competency name or O*NET standard code mappings.
     */
    private List<BehavioralIndicator> findIndicatorsForCompetencyName(String competencyName) {
        // First try to find competency by name
        List<Competency> matchingCompetencies = competencyRepository.findAll().stream()
            .filter(c -> {
                // Match by name
                if (c.getName().equalsIgnoreCase(competencyName)) {
                    return true;
                }
                // Match by O*NET code or title
                var standardCodes = c.getStandardCodes();
                if (standardCodes != null && standardCodes.hasOnetMapping()) {
                    var onetRef = standardCodes.onetRef();
                    return competencyName.equalsIgnoreCase(onetRef.code()) ||
                           competencyName.equalsIgnoreCase(onetRef.title());
                }
                return false;
            })
            .toList();

        if (matchingCompetencies.isEmpty()) {
            log.debug("No competency found matching name: {}", competencyName);
            return List.of();
        }

        // Get indicators for matching competencies
        return matchingCompetencies.stream()
            .flatMap(c -> indicatorRepository.findByCompetencyId(c.getId()).stream())
            .filter(BehavioralIndicator::isActive)
            .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Gap analysis info record.
     */
    private record GapInfo(
        String competencyName,
        double benchmark,
        double candidateScore,
        double gap,
        boolean isSignificant
    ) {}
}
