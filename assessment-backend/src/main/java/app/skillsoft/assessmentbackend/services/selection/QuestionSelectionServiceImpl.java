package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning.WarningLevel;
import app.skillsoft.assessmentbackend.domain.dto.simulation.WarningCode;
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
 * - Reproducible question ordering via seeded Random (BE-008)
 */
@Service
@Transactional(readOnly = true)
public class QuestionSelectionServiceImpl implements QuestionSelectionService {

    private static final Logger log = LoggerFactory.getLogger(QuestionSelectionServiceImpl.class);

    /**
     * Thread-local seeded Random for reproducible question ordering.
     * When set (via {@link #setSessionSeed(UUID)}), all shuffle operations
     * use this instance instead of ThreadLocalRandom, producing the same
     * question order for the same session ID.
     *
     * Must be cleared after use via {@link #clearSessionSeed()} to prevent
     * leaking state across pooled threads.
     */
    private static final ThreadLocal<Random> SEEDED_RANDOM = new ThreadLocal<>();

    private final AssessmentQuestionRepository questionRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final CompetencyRepository competencyRepository;
    private final PsychometricBlueprintValidator psychometricValidator;
    private final ItemStatisticsRepository itemStatisticsRepository;
    private final ExposureTrackingService exposureTrackingService;

    /**
     * Difficulty bands used for graduated difficulty distribution.
     * When questionsPerIndicator >= 3, questions are selected across these bands
     * to ensure a spread from foundational through advanced difficulty.
     */
    private static final DifficultyLevel[] GRADUATED_DIFFICULTY_BANDS = {
            DifficultyLevel.FOUNDATIONAL,
            DifficultyLevel.INTERMEDIATE,
            DifficultyLevel.ADVANCED
    };

    public QuestionSelectionServiceImpl(
            AssessmentQuestionRepository questionRepository,
            BehavioralIndicatorRepository indicatorRepository,
            CompetencyRepository competencyRepository,
            PsychometricBlueprintValidator psychometricValidator,
            ItemStatisticsRepository itemStatisticsRepository,
            ExposureTrackingService exposureTrackingService) {
        this.questionRepository = questionRepository;
        this.indicatorRepository = indicatorRepository;
        this.competencyRepository = competencyRepository;
        this.psychometricValidator = psychometricValidator;
        this.itemStatisticsRepository = itemStatisticsRepository;
        this.exposureTrackingService = exposureTrackingService;
    }

    // ========== REPRODUCIBLE RANDOM (BE-008) ==========

    @Override
    public void setSessionSeed(UUID sessionId) {
        if (sessionId == null) {
            log.debug("Null sessionId provided to setSessionSeed, shuffles will be non-deterministic");
            return;
        }
        Random seeded = new Random(sessionId.getMostSignificantBits());
        SEEDED_RANDOM.set(seeded);
        log.debug("Set seeded Random for session {} (seed={})",
                sessionId, sessionId.getMostSignificantBits());
    }

    @Override
    public void clearSessionSeed() {
        SEEDED_RANDOM.remove();
    }

    /**
     * Return the seeded Random if set, otherwise a new (non-deterministic) Random.
     * This ensures reproducible ordering when a session seed has been set,
     * while preserving existing behavior for callers that do not set a seed
     * (e.g., simulation, validation).
     */
    private Random getRandomInstance() {
        Random seeded = SEEDED_RANDOM.get();
        return seeded != null ? seeded : new Random();
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
            SelectionWarningCollector.addWarning(WarningLevel.WARNING,
                    WarningCode.NO_ACTIVE_QUESTIONS_INDICATOR,
                    "No active questions found for indicator " + indicatorId,
                    Map.of("indicatorId", indicatorId.toString()));
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
            // Shuffle if no difficulty preference (seeded for reproducibility)
            Collections.shuffle(eligible, getRandomInstance());
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
            Collections.shuffle(eligibleAnyDifficulty, getRandomInstance());

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
                String compName = indicator.getCompetency().getName() != null
                        ? indicator.getCompetency().getName() : competencyId.toString();
                log.warn("Fallback Tier 3: Indicator {} exhausted, selecting {} questions " +
                         "from sibling indicators of competency {} ({}) (flagged for psychometric review)",
                        indicatorId, remaining, compName, competencyId);

                int sizeBeforeBorrowing = result.size();

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
                    Collections.shuffle(eligibleSibling, getRandomInstance());

                    for (AssessmentQuestion q : eligibleSibling) {
                        if (remaining <= 0) break;
                        result.add(q.getId());
                        allExcluded.add(q.getId());
                        remaining--;
                    }
                }

                int actualBorrowed = result.size() - sizeBeforeBorrowing;
                if (actualBorrowed > 0) {
                    SelectionWarningCollector.addWarning(WarningLevel.WARNING,
                            WarningCode.INDICATOR_EXHAUSTED_BORROWING,
                            String.format("Borrowing %d questions from sibling indicators of %s (flagged for psychometric review)",
                                    actualBorrowed, compName),
                            Map.of("indicatorId", indicatorId.toString(),
                                   "count", String.valueOf(actualBorrowed),
                                   "competencyName", compName));
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
     * Waterfall distribution with context neutrality filtering and graduated difficulty.
     * Only selects questions that pass the context neutrality check (GENERAL tag,
     * UNIVERSAL tag, or no narrow domain tags).
     *
     * When {@code questionsPerRound >= 3}, applies graduated difficulty distribution:
     * each indicator gets questions spread across FOUNDATIONAL, INTERMEDIATE, and ADVANCED
     * bands. This ensures the OVERVIEW assessment probes multiple cognitive levels per
     * competency, producing a richer Competency Passport.
     *
     * When {@code questionsPerRound < 3}, uses the single {@code preferredDifficulty}
     * for all selections (legacy behavior).
     *
     * Used exclusively by OVERVIEW assessments for Competency Passport generation
     * where construct validity requires context-neutral items.
     */
    private List<UUID> selectWithWaterfallContextNeutral(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound,
            DifficultyLevel preferredDifficulty) {

        boolean useGraduatedDifficulty = questionsPerRound >= 3;

        if (useGraduatedDifficulty) {
            log.info("Using graduated difficulty distribution for OVERVIEW assembly " +
                    "(questionsPerRound={}, bands={})",
                    questionsPerRound, Arrays.toString(GRADUATED_DIFFICULTY_BANDS));
            return selectWithGraduatedDifficulty(indicatorIds, totalQuestions, questionsPerRound);
        }

        // Legacy single-difficulty path for questionsPerRound < 3
        return selectWithSingleDifficultyContextNeutral(
                indicatorIds, totalQuestions, questionsPerRound, preferredDifficulty);
    }

    /**
     * Graduated difficulty selection for context-neutral OVERVIEW waterfall.
     *
     * For each indicator, selects questions in difficulty bands:
     * - Band 1 (FOUNDATIONAL): 1 question
     * - Band 2 (INTERMEDIATE): 1 question
     * - Band 3 (ADVANCED): 1 question
     * - Remaining: cycle through bands again
     *
     * If a difficulty band has no questions for a given indicator, falls back
     * to any available difficulty to maintain the target count.
     */
    private List<UUID> selectWithGraduatedDifficulty(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound) {

        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();

        // For each indicator, build separate pools per difficulty band
        Map<UUID, Map<DifficultyLevel, List<UUID>>> indicatorBandPools = new LinkedHashMap<>();
        Map<UUID, List<UUID>> indicatorFallbackPools = new LinkedHashMap<>();

        for (UUID indicatorId : indicatorIds) {
            Map<DifficultyLevel, List<UUID>> bandPools = new LinkedHashMap<>();
            for (DifficultyLevel band : GRADUATED_DIFFICULTY_BANDS) {
                bandPools.put(band, getContextNeutralQuestionPool(indicatorId, band));
            }
            indicatorBandPools.put(indicatorId, bandPools);

            // Fallback pool: all context-neutral questions regardless of difficulty
            indicatorFallbackPools.put(indicatorId, getContextNeutralQuestionPool(indicatorId, null));
        }

        // Iterate over indicators in waterfall fashion
        // Each indicator gets questionsPerRound questions, spread across difficulty bands
        int maxRounds = (totalQuestions / Math.max(1, indicatorIds.size()) / Math.max(1, questionsPerRound)) + 1;

        for (int round = 0; round < maxRounds && selectedQuestions.size() < totalQuestions; round++) {
            for (UUID indicatorId : indicatorIds) {
                if (selectedQuestions.size() >= totalQuestions) break;

                Map<DifficultyLevel, List<UUID>> bandPools = indicatorBandPools.get(indicatorId);
                int questionsThisRound = 0;
                int targetForRound = Math.max(1, questionsPerRound);

                // Phase 1: Select one question per difficulty band (graduated distribution)
                for (DifficultyLevel band : GRADUATED_DIFFICULTY_BANDS) {
                    if (questionsThisRound >= targetForRound) break;
                    if (selectedQuestions.size() >= totalQuestions) break;

                    List<UUID> pool = bandPools.get(band);
                    UUID selected = pickFirstUnused(pool, usedQuestions);

                    if (selected != null) {
                        selectedQuestions.add(selected);
                        usedQuestions.add(selected);
                        questionsThisRound++;
                    } else {
                        log.debug("No {} question available for indicator {} in round {}, " +
                                "will use fallback", band, indicatorId, round);
                    }
                }

                // Phase 2: Fill remaining slots from any difficulty (fallback)
                if (questionsThisRound < targetForRound) {
                    List<UUID> fallbackPool = indicatorFallbackPools.get(indicatorId);
                    while (questionsThisRound < targetForRound
                            && selectedQuestions.size() < totalQuestions) {
                        UUID selected = pickFirstUnused(fallbackPool, usedQuestions);
                        if (selected == null) break; // indicator exhausted

                        selectedQuestions.add(selected);
                        usedQuestions.add(selected);
                        questionsThisRound++;

                        log.debug("Fallback: selected any-difficulty question for indicator {} " +
                                "(round {}, slot {})", indicatorId, round, questionsThisRound);
                    }
                }

                // Phase 3: If questionsPerRound > 3, fill extra slots by cycling bands again
                if (questionsThisRound < targetForRound) {
                    int bandIdx = 0;
                    while (questionsThisRound < targetForRound
                            && selectedQuestions.size() < totalQuestions) {
                        DifficultyLevel band = GRADUATED_DIFFICULTY_BANDS[bandIdx % GRADUATED_DIFFICULTY_BANDS.length];
                        List<UUID> pool = bandPools.get(band);
                        UUID selected = pickFirstUnused(pool, usedQuestions);

                        if (selected != null) {
                            selectedQuestions.add(selected);
                            usedQuestions.add(selected);
                            questionsThisRound++;
                        }
                        bandIdx++;

                        // Safety: break if we have cycled through all bands without finding anything
                        if (bandIdx >= GRADUATED_DIFFICULTY_BANDS.length * 2) break;
                    }
                }
            }
        }

        log.info("Graduated difficulty context-neutral waterfall selected {} questions " +
                "across {} indicators (target: {})",
                selectedQuestions.size(), indicatorIds.size(), totalQuestions);

        return selectedQuestions;
    }

    /**
     * Pick the first unused question ID from the pool.
     *
     * @param pool         Ordered list of candidate question IDs
     * @param usedQuestions Set of already-selected question IDs
     * @return The first unused ID, or null if pool is exhausted
     */
    private UUID pickFirstUnused(List<UUID> pool, Set<UUID> usedQuestions) {
        for (UUID id : pool) {
            if (!usedQuestions.contains(id)) {
                return id;
            }
        }
        return null;
    }

    /**
     * Legacy single-difficulty context-neutral waterfall selection.
     * Used when questionsPerRound < 3 (graduated difficulty not applicable).
     */
    private List<UUID> selectWithSingleDifficultyContextNeutral(
            List<UUID> indicatorIds,
            int totalQuestions,
            int questionsPerRound,
            DifficultyLevel preferredDifficulty) {

        List<UUID> selectedQuestions = new ArrayList<>();
        Set<UUID> usedQuestions = new HashSet<>();
        Map<UUID, List<UUID>> indicatorQuestionPool = new HashMap<>();
        Map<UUID, Integer> indicatorCursors = new HashMap<>();

        for (UUID indicatorId : indicatorIds) {
            List<UUID> pool = getContextNeutralQuestionPool(indicatorId, preferredDifficulty);
            indicatorQuestionPool.put(indicatorId, pool);
            indicatorCursors.put(indicatorId, 0);
        }

        int maxRounds = questionsPerRound > 0
                ? (totalQuestions / indicatorIds.size() / questionsPerRound) + 1
                : totalQuestions;

        for (int round = 0; round < maxRounds && selectedQuestions.size() < totalQuestions; round++) {
            for (UUID indicatorId : indicatorIds) {
                if (selectedQuestions.size() >= totalQuestions) break;

                List<UUID> pool = indicatorQuestionPool.get(indicatorId);
                int cursor = indicatorCursors.get(indicatorId);
                int questionsThisRound = 0;

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

        log.debug("Single-difficulty context-neutral waterfall selected {} questions across {} indicators",
                selectedQuestions.size(), indicatorIds.size());

        return selectedQuestions;
    }

    /**
     * Get a pool of eligible, context-neutral question IDs for an indicator.
     * Filters by validity AND context neutrality (GENERAL/UNIVERSAL tags only).
     */
    private List<UUID> getContextNeutralQuestionPool(UUID indicatorId, DifficultyLevel preferredDifficulty) {
        List<AssessmentQuestion> candidates = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        // Filter by validity, then by context neutrality
        List<AssessmentQuestion> eligible = filterByValidity(candidates);
        eligible = filterByContextNeutrality(eligible);

        if (preferredDifficulty != null) {
            eligible = applyDifficultyPreferenceWithExposureControl(eligible, preferredDifficulty);
        } else {
            eligible = new ArrayList<>(eligible);
            eligible.sort(Comparator.comparingInt(AssessmentQuestion::getExposureCount));
        }

        return eligible.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toList());
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
            String compName = competencyRepository.findById(competencyId)
                    .map(Competency::getName).orElse(competencyId.toString());
            log.warn("No active behavioral indicators found for competency {} ({})", compName, competencyId);
            SelectionWarningCollector.addWarning(WarningLevel.WARNING,
                    WarningCode.NO_ACTIVE_INDICATORS_COMPETENCY,
                    "No active behavioral indicators found for competency " + compName,
                    Map.of("competencyName", compName));
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
        return selectQuestionsForCompetencies(
                competencyIds, questionsPerIndicator, preferredDifficulty, shuffle, false);
    }

    @Override
    public List<UUID> selectQuestionsForCompetencies(
            List<UUID> competencyIds,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle,
            boolean contextNeutralOnly) {

        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("No competency IDs provided");
            return List.of();
        }

        log.info("Selecting questions for {} competencies ({} per indicator, difficulty: {}, contextNeutral: {})",
                competencyIds.size(), questionsPerIndicator, preferredDifficulty, contextNeutralOnly);

        // Batch-load all indicators for all competencies in a single query (N+1 fix)
        List<BehavioralIndicator> allIndicators = indicatorRepository
                .findByCompetencyIdIn(new HashSet<>(competencyIds))
                .stream()
                .filter(BehavioralIndicator::isActive)
                .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
                .toList();

        if (allIndicators.isEmpty()) {
            List<String> compNames = competencyRepository.findAllById(competencyIds).stream()
                    .map(Competency::getName).toList();
            String namesJoined = compNames.isEmpty()
                    ? competencyIds.size() + " competencies" : String.join(", ", compNames);
            log.warn("No active behavioral indicators found for competencies: {}", namesJoined);
            SelectionWarningCollector.addWarning(WarningLevel.WARNING,
                    WarningCode.NO_ACTIVE_INDICATORS_COMPETENCIES,
                    "No active behavioral indicators found for: " + namesJoined,
                    Map.of("count", String.valueOf(competencyIds.size()),
                           "competencyNames", namesJoined));
            return List.of();
        }

        List<UUID> indicatorIds = allIndicators.stream()
                .map(BehavioralIndicator::getId)
                .toList();

        // Calculate total questions: indicators * questionsPerIndicator
        int totalQuestions = indicatorIds.size() * questionsPerIndicator;

        List<UUID> selectedQuestions;
        if (contextNeutralOnly) {
            // For OVERVIEW assessments: apply context neutrality filtering in waterfall selection
            selectedQuestions = selectWithWaterfallContextNeutral(
                    indicatorIds, totalQuestions, questionsPerIndicator, preferredDifficulty);
        } else {
            selectedQuestions = selectQuestionsWithDistribution(
                    indicatorIds,
                    totalQuestions,
                    questionsPerIndicator,
                    DistributionStrategy.WATERFALL,
                    preferredDifficulty
            );
        }

        // Auto-track exposure via dedicated service (avoids self-invocation @Transactional issue)
        if (!selectedQuestions.isEmpty()) {
            exposureTrackingService.trackExposure(selectedQuestions);
        }

        // Shuffle if requested (prevents clustering by competency)
        if (shuffle && !selectedQuestions.isEmpty()) {
            List<UUID> shuffled = new ArrayList<>(selectedQuestions);
            Collections.shuffle(shuffled, getRandomInstance());
            log.debug("Shuffled {} selected questions", shuffled.size());
            return shuffled;
        }

        return selectedQuestions;
    }

    @Override
    public List<UUID> selectQuestionsForCompetenciesWeighted(
            List<UUID> competencyIds,
            Map<UUID, Double> competencyWeights,
            int questionsPerIndicator,
            DifficultyLevel preferredDifficulty,
            boolean shuffle,
            boolean contextNeutralOnly) {

        if (competencyIds == null || competencyIds.isEmpty()) {
            log.warn("No competency IDs provided for weighted selection");
            return List.of();
        }

        log.info("Selecting weighted questions for {} competencies ({} per indicator, weights: {})",
                competencyIds.size(), questionsPerIndicator, competencyWeights);

        // Batch-load all indicators for all competencies
        List<BehavioralIndicator> allIndicators = indicatorRepository
                .findByCompetencyIdIn(new HashSet<>(competencyIds))
                .stream()
                .filter(BehavioralIndicator::isActive)
                .sorted(Comparator.comparing(BehavioralIndicator::getWeight).reversed())
                .toList();

        if (allIndicators.isEmpty()) {
            List<String> compNames = competencyRepository.findAllById(competencyIds).stream()
                    .map(Competency::getName).toList();
            String namesJoined = compNames.isEmpty()
                    ? competencyIds.size() + " competencies" : String.join(", ", compNames);
            log.warn("No active behavioral indicators found for weighted competencies: {}", namesJoined);
            SelectionWarningCollector.addWarning(WarningLevel.WARNING,
                    WarningCode.NO_ACTIVE_INDICATORS_COMPETENCIES,
                    "No active behavioral indicators found for: " + namesJoined,
                    Map.of("count", String.valueOf(competencyIds.size()),
                           "competencyNames", namesJoined));
            return List.of();
        }

        // Build indicator-level weights: indicator.weight * competencyWeight[competencyId]
        Map<UUID, Double> indicatorWeights = new LinkedHashMap<>();
        for (BehavioralIndicator bi : allIndicators) {
            UUID compId = bi.getCompetency().getId();
            double compWeight = competencyWeights.getOrDefault(compId, 1.0);
            indicatorWeights.put(bi.getId(), bi.getWeight() * compWeight);
        }

        int totalQuestions = allIndicators.size() * questionsPerIndicator;

        List<UUID> selectedQuestions = selectQuestionsWeighted(
                indicatorWeights, totalQuestions, preferredDifficulty);

        // Auto-track exposure
        if (!selectedQuestions.isEmpty()) {
            exposureTrackingService.trackExposure(selectedQuestions);
        }

        // Shuffle if requested
        if (shuffle && !selectedQuestions.isEmpty()) {
            List<UUID> shuffled = new ArrayList<>(selectedQuestions);
            Collections.shuffle(shuffled, getRandomInstance());
            log.debug("Shuffled {} weighted-selected questions", shuffled.size());
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

        // Batch-load item statistics for all questions in a single query (N+1 fix).
        // Questions are already confirmed active (fetched via findByBehavioralIndicator_IdAndIsActiveTrue),
        // so we only need to check validity status -- skip redundant questionRepository.findById().
        Set<UUID> questionIds = questions.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toSet());

        // Build a map of questionId -> validityStatus (default PROBATION if no stats exist)
        Map<UUID, ItemValidityStatus> statusByQuestionId = itemStatisticsRepository
                .findByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(
                        ItemStatistics::getQuestionId,
                        ItemStatistics::getValidityStatus,
                        (a, b) -> a
                ));

        return questions.stream()
                .filter(q -> {
                    // Default to PROBATION if no statistics exist (same as PsychometricBlueprintValidator)
                    ItemValidityStatus status = statusByQuestionId
                            .getOrDefault(q.getId(), ItemValidityStatus.PROBATION);
                    // RETIRED items are never eligible for assembly
                    return status != ItemValidityStatus.RETIRED;
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
            Collections.shuffle(shuffled, getRandomInstance());
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

}
