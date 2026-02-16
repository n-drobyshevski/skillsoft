package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.services.validation.PsychometricBlueprintValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of QuestionSelectionService.
 *
 * Consolidates question selection logic extracted from:
 * - TestSessionServiceImpl.generateScenarioAOrder() / generateLegacyQuestionOrder()
 * - OverviewAssembler.collectQuestionsForIndicators() / applyWaterfallDistribution()
 * - JobFitAssembler.selectQuestionsForGaps()
 * - TeamFitAssembler.selectQuestionsForCompetencies()
 *
 * Key improvements over scattered implementations:
 * - Centralized psychometric validation
 * - Consistent difficulty fallback logic
 * - Reusable distribution strategies
 * - Proper logging and metrics
 */
@Service
@Transactional(readOnly = true)
public class QuestionSelectionServiceImpl implements QuestionSelectionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionSelectionServiceImpl.class);

    private final AssessmentQuestionRepository questionRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final PsychometricBlueprintValidator psychometricValidator;
    private final ItemStatisticsRepository itemStatisticsRepository;

    public QuestionSelectionServiceImpl(
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository,
            PsychometricBlueprintValidator psychometricValidator,
            ItemStatisticsRepository itemStatisticsRepository) {
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
        this.psychometricValidator = psychometricValidator;
        this.itemStatisticsRepository = itemStatisticsRepository;
    }

    // ========== SINGLE INDICATOR SELECTION ==========

    @Override
    public List<UUID> selectQuestionsForIndicator(
            UUID indicatorId,
            int maxQuestions,
            DifficultyLevel preferredDifficulty,
            Set<UUID> excludeQuestionIds) {

        log.debug("Selecting up to {} questions for indicator {} (difficulty: {}, excluding: {})",
                maxQuestions, indicatorId, preferredDifficulty, excludeQuestionIds.size());

        // Fetch all active questions for the indicator
        List<AssessmentQuestion> candidates = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        if (candidates.isEmpty()) {
            log.warn("No active questions found for indicator {}", indicatorId);
            return List.of();
        }

        // Apply psychometric filtering (exclude RETIRED items)
        List<AssessmentQuestion> eligible = filterByValidity(candidates);

        // Exclude already-selected questions
        if (!excludeQuestionIds.isEmpty()) {
            eligible = eligible.stream()
                    .filter(q -> !excludeQuestionIds.contains(q.getId()))
                    .collect(Collectors.toList());
        }

        // Apply difficulty preference
        if (preferredDifficulty != null) {
            eligible = applyDifficultyPreference(eligible, preferredDifficulty);
        } else {
            // Shuffle if no difficulty preference (random selection)
            Collections.shuffle(eligible);
        }

        // Select up to maxQuestions
        List<UUID> selected = eligible.stream()
                .limit(maxQuestions)
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toList());

        // Three-tier fallback if insufficient questions found
        if (selected.size() < maxQuestions) {
            selected = applyExhaustionFallback(
                    indicatorId, maxQuestions, preferredDifficulty,
                    excludeQuestionIds, selected);
        }

        log.debug("Selected {} questions for indicator {} (requested: {})",
                selected.size(), indicatorId, maxQuestions);

        return selected;
    }

    /**
     * Three-tier fallback strategy when an indicator has insufficient questions:
     * Tier 1: Exact difficulty match (already applied above)
     * Tier 2: Any difficulty, same indicator (broaden difficulty search)
     * Tier 3: Cross-indicator, same competency (with log warning for psychometric review)
     *
     * @return Enhanced list of question IDs
     */
    private List<UUID> applyExhaustionFallback(
            UUID indicatorId,
            int maxQuestions,
            DifficultyLevel preferredDifficulty,
            Set<UUID> excludeQuestionIds,
            List<UUID> alreadySelected) {

        List<UUID> result = new ArrayList<>(alreadySelected);
        Set<UUID> allExcluded = new HashSet<>(excludeQuestionIds);
        allExcluded.addAll(result);
        int remaining = maxQuestions - result.size();

        // Tier 2: Any difficulty, same indicator
        if (remaining > 0 && preferredDifficulty != null) {
            log.debug("Fallback Tier 2: Selecting {} questions for indicator {} with ANY difficulty",
                    remaining, indicatorId);

            List<AssessmentQuestion> allDifficultyQuestions = questionRepository
                    .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);
            List<AssessmentQuestion> eligibleAnyDifficulty = filterByValidity(allDifficultyQuestions)
                    .stream()
                    .filter(q -> !allExcluded.contains(q.getId()))
                    .collect(Collectors.toList());
            Collections.shuffle(eligibleAnyDifficulty);

            for (AssessmentQuestion q : eligibleAnyDifficulty) {
                if (remaining <= 0) break;
                result.add(q.getId());
                allExcluded.add(q.getId());
                remaining--;
            }
        }

        // Tier 3: Cross-indicator, same competency
        if (remaining > 0) {
            BehavioralIndicator indicator = indicatorRepository.findById(indicatorId).orElse(null);
            if (indicator != null && indicator.getCompetency() != null) {
                UUID competencyId = indicator.getCompetency().getId();
                log.warn("Fallback Tier 3: Indicator {} exhausted, selecting {} questions " +
                         "from sibling indicators of competency {} (flagged for psychometric review)",
                        indicatorId, remaining, competencyId);

                List<BehavioralIndicator> siblings = indicatorRepository.findByCompetencyId(competencyId)
                        .stream()
                        .filter(i -> !i.getId().equals(indicatorId) && i.isActive())
                        .toList();

                for (BehavioralIndicator sibling : siblings) {
                    if (remaining <= 0) break;

                    List<AssessmentQuestion> siblingQuestions = questionRepository
                            .findByBehavioralIndicator_IdAndIsActiveTrue(sibling.getId());
                    List<AssessmentQuestion> eligibleSibling = filterByValidity(siblingQuestions)
                            .stream()
                            .filter(q -> !allExcluded.contains(q.getId()))
                            .collect(Collectors.toList());
                    Collections.shuffle(eligibleSibling);

                    for (AssessmentQuestion q : eligibleSibling) {
                        if (remaining <= 0) break;
                        result.add(q.getId());
                        allExcluded.add(q.getId());
                        remaining--;
                    }
                }
            }
        }

        return result;
    }

    // ========== MULTI-INDICATOR DISTRIBUTION ==========

    @Override
    public List<UUID> selectQuestionsWithDistribution(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound,
            DistributionStrategy strategy,
            DifficultyLevel preferredDifficulty) {

        if (indicatorIds == null || indicatorIds.isEmpty()) {
            log.warn("No indicator IDs provided for distribution selection");
            return List.of();
        }

        log.info("Selecting {} questions across {} indicators (strategy: {}, difficulty: {})",
                totalQuestions, indicatorIds.size(), strategy, preferredDifficulty);

        return switch (strategy) {
            case WATERFALL -> selectWithWaterfall(indicatorIds, totalQuestions, questionsPerRound, preferredDifficulty);
            case WEIGHTED -> {
                // For weighted without explicit weights, use equal weights
                Map<UUID, Double> equalWeights = indicatorIds.stream()
                        .collect(Collectors.toMap(id -> id, id -> 1.0));
                yield selectQuestionsWeighted(equalWeights, totalQuestions, preferredDifficulty);
            }
            case PRIORITY_FIRST -> selectWithPriorityFirst(indicatorIds, totalQuestions, questionsPerRound, preferredDifficulty);
        };
    }

    /**
     * Waterfall distribution: cycle through indicators in rounds.
     *
     * Round 1: Indicator1-Q1, Indicator2-Q1, Indicator3-Q1...
     * Round 2: Indicator1-Q2, Indicator2-Q2, Indicator3-Q2...
     *
     * Ensures balanced coverage even if total limit is reached early.
     */
    private List<UUID> selectWithWaterfall(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound,
            DifficultyLevel preferredDifficulty) {

        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();
        Map<UUID, List<UUID>> indicatorQuestionPool = new HashMap<>();
        Map<UUID, Integer> indicatorCursors = new HashMap<>();

        // Prepare question pools for each indicator
        for (UUID indicatorId : indicatorIds) {
            List<UUID> pool = getEligibleQuestionPool(indicatorId, preferredDifficulty);
            indicatorQuestionPool.put(indicatorId, pool);
            indicatorCursors.put(indicatorId, 0);
        }

        // Calculate number of rounds needed
        int maxRounds = questionsPerRound > 0
                ? (totalQuestions / indicatorIds.size() / questionsPerRound) + 1
                : totalQuestions;

        // Waterfall iteration
        for (int round = 0; round < maxRounds && selectedQuestions.size() < totalQuestions; round++) {
            for (UUID indicatorId : indicatorIds) {
                if (selectedQuestions.size() >= totalQuestions) break;

                List<UUID> pool = indicatorQuestionPool.get(indicatorId);
                int cursor = indicatorCursors.get(indicatorId);
                int questionsThisRound = 0;

                // Select questions for this indicator in this round
                while (cursor < pool.size() && questionsThisRound < Math.max(1, questionsPerRound)) {
                    UUID questionId = pool.get(cursor);
                    cursor++;

                    if (!usedQuestions.contains(questionId)) {
                        selectedQuestions.add(questionId);
                        usedQuestions.add(questionId);
                        questionsThisRound++;

                        if (selectedQuestions.size() >= totalQuestions) break;
                    }
                }

                indicatorCursors.put(indicatorId, cursor);
            }
        }

        log.debug("Waterfall distribution selected {} questions across {} indicators",
                selectedQuestions.size(), indicatorIds.size());

        return selectedQuestions;
    }

    /**
     * Priority-first distribution: fill highest-priority indicators first.
     *
     * Each indicator gets its full allocation before moving to the next.
     * Order of indicatorIds determines priority.
     */
    private List<UUID> selectWithPriorityFirst(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty) {

        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();

        for (UUID indicatorId : indicatorIds) {
            if (selectedQuestions.size() >= totalQuestions) break;

            int remaining = totalQuestions - selectedQuestions.size();
            int toSelect = Math.min(questionsPerIndicator, remaining);

            List<UUID> questions = selectQuestionsForIndicator(
                    indicatorId, toSelect, preferredDifficulty, usedQuestions);

            selectedQuestions.addAll(questions);
            usedQuestions.addAll(questions);
        }

        log.debug("Priority-first distribution selected {} questions across {} indicators",
                selectedQuestions.size(), indicatorIds.size());

        return selectedQuestions;
    }

    @Override
    public List<UUID> selectQuestionsWeighted(
            Map<UUID, Double> indicatorWeights,
            int totalQuestions,
            DifficultyLevel preferredDifficulty) {

        if (indicatorWeights == null || indicatorWeights.isEmpty()) {
            log.warn("No indicator weights provided for weighted selection");
            return List.of();
        }

        // Calculate total weight
        double totalWeight = indicatorWeights.values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (totalWeight <= 0) {
            log.warn("Total weight is zero or negative");
            return List.of();
        }

        // Calculate question allocation per indicator
        Map<UUID, Integer> allocation = new LinkedHashMap<>();
        int allocated = 0;

        List<Map.Entry<UUID, Double>> sortedEntries = indicatorWeights.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .toList();

        for (Map.Entry<UUID, Double> entry : sortedEntries) {
            UUID indicatorId = entry.getKey();
            double weight = entry.getValue();

            int questionsForIndicator = (int) Math.round((weight / totalWeight) * totalQuestions);
            // Ensure at least 1 question for non-zero weights
            questionsForIndicator = Math.max(1, questionsForIndicator);
            // Don't exceed remaining
            questionsForIndicator = Math.min(questionsForIndicator, totalQuestions - allocated);

            allocation.put(indicatorId, questionsForIndicator);
            allocated += questionsForIndicator;
        }

        // Select questions according to allocation
        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();

        for (Map.Entry<UUID, Integer> entry : allocation.entrySet()) {
            UUID indicatorId = entry.getKey();
            int toSelect = entry.getValue();

            List<UUID> questions = selectQuestionsForIndicator(
                    indicatorId, toSelect, preferredDifficulty, usedQuestions);

            selectedQuestions.addAll(questions);
            usedQuestions.addAll(questions);
        }

        log.debug("Weighted distribution selected {} questions across {} indicators",
                selectedQuestions.size(), indicatorWeights.size());

        return selectedQuestions;
    }

    // ========== COMPETENCY-LEVEL SELECTION ==========

    @Override
    public List<UUID> selectQuestionsForCompetency(
            UUID competencyId,
            int totalQuestions,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty) {

        // Get all active indicators for this competency, sorted by weight
        List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(competencyId)
                .stream()
                .filter(BehavioralIndicator::isActive)
                .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
                .toList();

        if (indicators.isEmpty()) {
            log.warn("No active behavioral indicators found for competency {}", competencyId);
            return List.of();
        }

        List<UUID> indicatorIds = indicators.stream()
                .map(BehavioralIndicator::getId)
                .toList();

        return selectQuestionsWithDistribution(
                indicatorIds,
                totalQuestions,
                questionsPerIndicator,
                DistributionStrategy.WATERFALL,
                preferredDifficulty
        );
    }

    @Override
    public List<UUID> selectQuestionsForCompetencies(
            List<UUID> competencyIds,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle) {

        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("No competency IDs provided");
            return List.of();
        }

        log.info("Selecting questions for {} competencies ({} per indicator, difficulty: {})",
                competencyIds.size(), questionsPerIndicator, preferredDifficulty);

        // Gather all indicators from all competencies, sorted by weight
        List<BehavioralIndicator> allIndicators = competencyIds.stream()
                .flatMap(compId -> indicatorRepository.findByCompetencyId(compId).stream())
                .filter(BehavioralIndicator::isActive)
                .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
                .toList();

        if (allIndicators.isEmpty()) {
            log.warn("No active behavioral indicators found for competencies: {}", competencyIds);
            return List.of();
        }

        List<UUID> indicatorIds = allIndicators.stream()
                .map(BehavioralIndicator::getId)
                .toList();

        // Calculate total questions: indicators * questionsPerIndicator
        int totalQuestions = indicatorIds.size() * questionsPerIndicator;

        List<UUID> selectedQuestions = selectQuestionsWithDistribution(
                indicatorIds,
                totalQuestions,
                questionsPerIndicator,
                DistributionStrategy.WATERFALL,
                preferredDifficulty
        );

        // Shuffle if requested (prevents clustering by competency)
        if (shuffle && !selectedQuestions.isEmpty()) {
            List<UUID> shuffled = new ArrayList<>(selectedQuestions);
            Collections.shuffle(shuffled);
            log.debug("Shuffled {} selected questions", shuffled.size());
            return shuffled;
        }

        return selectedQuestions;
    }

    // ========== FILTERING UTILITIES ==========

    @Override
    public List<AssessmentQuestion> filterByValidity(
            List<AssessmentQuestion> questions,
            Set<ItemValidityStatus> allowedStatuses) {

        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        return questions.stream()
                .filter(q -> {
                    // Use psychometric validator to check eligibility
                    // This respects RETIRED status exclusion
                    return psychometricValidator.isEligibleForAssembly(q.getId());
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<AssessmentQuestion> applyDifficultyPreference(
            List<AssessmentQuestion> questions,
            DifficultyLevel preferred) {

        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        if (preferred == null) {
            // No preference - shuffle and return
            List<AssessmentQuestion> shuffled = new ArrayList<>(questions);
            Collections.shuffle(shuffled);
            return shuffled;
        }

        // Sort by difficulty preference: preferred first, then adjacent, then others
        return questions.stream()
                .sorted(Comparator.comparing(q -> getDifficultyPriority(q.getDifficultyLevel(), preferred)))
                .collect(Collectors.toList());
    }

    /**
     * Calculate priority for difficulty levels relative to preferred.
     * Lower values = higher priority.
     *
     * Priority order:
     * 0 - Exact match
     * 1 - Adjacent difficulty (one level away)
     * 2 - Two levels away
     * 3+ - Further away
     */
    private int getDifficultyPriority(DifficultyLevel actual, DifficultyLevel preferred) {
        if (actual == preferred) return 0;
        if (actual == null || preferred == null) return 3;

        // Calculate distance based on ordinal values
        int distance = Math.abs(actual.ordinal() - preferred.ordinal());
        return distance;
    }

    @Override
    public List<AssessmentQuestion> filterByContextNeutrality(List<AssessmentQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return List.of();
        }

        return questions.stream()
                .filter(AssessmentQuestion::isContextNeutral)
                .collect(Collectors.toList());
    }

    // ========== ELIGIBILITY CHECKS ==========

    @Override
    public boolean isEligibleForAssembly(UUID questionId) {
        return psychometricValidator.isEligibleForAssembly(questionId);
    }

    @Override
    public int getEligibleQuestionCount(UUID indicatorId) {
        List<AssessmentQuestion> questions = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        return (int) questions.stream()
                .filter(q -> psychometricValidator.isEligibleForAssembly(q.getId()))
                .count();
    }

    @Override
    public int getEligibleQuestionCountForCompetency(UUID competencyId) {
        List<BehavioralIndicator> indicators = indicatorRepository.findByCompetencyId(competencyId);

        return indicators.stream()
                .filter(BehavioralIndicator::isActive)
                .mapToInt(ind -> getEligibleQuestionCount(ind.getId()))
                .sum();
    }

    // ========== HELPER METHODS ==========

    /**
     * Get a pool of eligible question IDs for an indicator, sorted by difficulty preference
     * and exposure count (less-exposed items preferred at equal difficulty).
     */
    private List<UUID> getEligibleQuestionPool(UUID indicatorId, DifficultyLevel preferredDifficulty) {
        List<AssessmentQuestion> candidates = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        // Filter by validity
        List<AssessmentQuestion> eligible = filterByValidity(candidates);

        // Apply difficulty preference with exposure-aware tiebreaking
        if (preferredDifficulty != null) {
            eligible = applyDifficultyPreferenceWithExposureControl(eligible, preferredDifficulty);
        } else {
            // Sort by exposure count (less-exposed first), then shuffle within same count
            eligible = new ArrayList<>(eligible);
            eligible.sort(Comparator.comparingInt(AssessmentQuestion::getExposureCount));
        }

        return eligible.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toList());
    }

    /**
     * Apply difficulty preference with exposure control and quality-aware sorting.
     * Questions are sorted by:
     *   (1) difficulty priority (exact match first, then adjacent)
     *   (2) discrimination index DESC (prefer high-discrimination items)
     *   (3) exposure count ASC (prefer less-exposed items)
     *
     * Batch-loads ItemStatistics to avoid N+1 queries when sorting by discrimination.
     */
    private List<AssessmentQuestion> applyDifficultyPreferenceWithExposureControl(
            List<AssessmentQuestion> questions, DifficultyLevel preferred) {

        if (questions == null || questions.isEmpty()) return List.of();

        // Batch-load item statistics for discrimination index sorting
        Set<UUID> questionIds = questions.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toSet());
        Map<UUID, BigDecimal> discriminationByQuestionId = loadDiscriminationIndex(questionIds);

        return questions.stream()
                .sorted(Comparator
                        .comparingInt((AssessmentQuestion q) ->
                                getDifficultyPriority(q.getDifficultyLevel(), preferred))
                        .thenComparing((AssessmentQuestion q) ->
                                discriminationByQuestionId.getOrDefault(q.getId(), BigDecimal.ZERO),
                                Comparator.reverseOrder())
                        .thenComparingInt(AssessmentQuestion::getExposureCount))
                .collect(Collectors.toList());
    }

    /**
     * Batch-load discrimination index values for a set of question IDs.
     * Returns a map of questionId -> discriminationIndex (defaults to ZERO if no stats).
     */
    private Map<UUID, BigDecimal> loadDiscriminationIndex(Set<UUID> questionIds) {
        if (questionIds.isEmpty()) return Map.of();

        List<ItemStatistics> statsList = itemStatisticsRepository.findByQuestionIdIn(questionIds);
        return statsList.stream()
                .filter(s -> s.getDiscriminationIndex() != null)
                .collect(Collectors.toMap(
                        ItemStatistics::getQuestionId,
                        ItemStatistics::getDiscriminationIndex,
                        (a, b) -> a  // in case of duplicates, keep first
                ));
    }

    /**
     * Increment exposure count for selected questions.
     * Should be called after questions are finalized for a test assembly.
     *
     * @param questionIds The IDs of questions selected for the test
     */
    public void trackExposure(List<UUID> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;

        List<AssessmentQuestion> questions = questionRepository.findAllById(questionIds);
        for (AssessmentQuestion q : questions) {
            q.incrementExposureCount();
        }
        questionRepository.saveAll(questions);
        log.debug("Incremented exposure count for {} questions", questions.size());
    }
}
