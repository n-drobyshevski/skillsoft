package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.ItemStatistics;
import app.skillsoft.assessmentbackend.domain.entities.ItemValidityStatus;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Validator that considers psychometric validity when selecting questions for test assembly.
 *
 * Per the Test Validation Mechanic architecture:
 * - Prioritizes ACTIVE (validated) items with good psychometric properties
 * - Includes a percentage of PROBATION items to gather data
 * - Never includes RETIRED items (toxic/deactivated)
 * - Can include FLAGGED_FOR_REVIEW items sparingly
 *
 * Selection Priority:
 * 1. ACTIVE items (rpb >= 0.3, p in acceptable range)
 * 2. PROBATION items (mix in ~20% to gather data)
 * 3. FLAGGED_FOR_REVIEW items (only if insufficient active/probation)
 * 4. NEVER include RETIRED items
 */
@Service
public class PsychometricBlueprintValidator {

    private static final Logger log = LoggerFactory.getLogger(PsychometricBlueprintValidator.class);

    private final ItemStatisticsRepository itemStatsRepository;
    private final AssessmentQuestionRepository questionRepository;

    @Value("${skillsoft.psychometrics.enabled:true}")
    private boolean psychometricsEnabled;

    @Value("${skillsoft.psychometrics.probation-percentage:20}")
    private int probationPercentage;

    public PsychometricBlueprintValidator(
            ItemStatisticsRepository itemStatsRepository,
            AssessmentQuestionRepository questionRepository) {
        this.itemStatsRepository = itemStatsRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Select validated questions for a set of behavioral indicators.
     * Considers psychometric validity status when selecting.
     *
     * @param indicatorIds List of behavioral indicator UUIDs
     * @param questionsPerIndicator Number of questions to select per indicator
     * @return List of selected question UUIDs
     */
    public List<UUID> selectValidatedQuestions(List<UUID> indicatorIds, int questionsPerIndicator) {
        if (!psychometricsEnabled) {
            return selectWithoutValidation(indicatorIds, questionsPerIndicator);
        }

        List<UUID> selectedQuestions = new ArrayList<>();

        for (UUID indicatorId : indicatorIds) {
            List<UUID> questionsForIndicator = selectQuestionsForIndicator(indicatorId, questionsPerIndicator);
            selectedQuestions.addAll(questionsForIndicator);
        }

        return selectedQuestions;
    }

    /**
     * Select questions for a single behavioral indicator with psychometric validation.
     */
    private List<UUID> selectQuestionsForIndicator(UUID indicatorId, int targetCount) {
        // Get all active questions for this indicator
        List<AssessmentQuestion> availableQuestions = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        if (availableQuestions.isEmpty()) {
            log.warn("No active questions found for indicator {}", indicatorId);
            return Collections.emptyList();
        }

        // Partition questions by validity status
        Map<ItemValidityStatus, List<AssessmentQuestion>> byStatus = partitionByValidityStatus(availableQuestions);

        List<AssessmentQuestion> activeItems = byStatus.getOrDefault(ItemValidityStatus.ACTIVE, Collections.emptyList());
        List<AssessmentQuestion> probationItems = byStatus.getOrDefault(ItemValidityStatus.PROBATION, Collections.emptyList());
        List<AssessmentQuestion> flaggedItems = byStatus.getOrDefault(ItemValidityStatus.FLAGGED_FOR_REVIEW, Collections.emptyList());
        // RETIRED items are explicitly excluded

        // Build selection pool with priorities
        List<AssessmentQuestion> selectionPool = new ArrayList<>();

        // Calculate how many probation items to include (~20% of target)
        int probationTarget = Math.max(1, (targetCount * probationPercentage) / 100);

        // Priority 1: Active items (main pool)
        Collections.shuffle(activeItems);
        selectionPool.addAll(activeItems);

        // Priority 2: Probation items (limited to probation target)
        Collections.shuffle(probationItems);
        int probationCount = Math.min(probationItems.size(), probationTarget);
        selectionPool.addAll(probationItems.subList(0, probationCount));

        // Priority 3: Flagged items (only if we still need more)
        if (selectionPool.size() < targetCount) {
            Collections.shuffle(flaggedItems);
            selectionPool.addAll(flaggedItems);
        }

        // Select up to targetCount questions
        return selectionPool.stream()
                .limit(targetCount)
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toList());
    }

    /**
     * Partition questions by their psychometric validity status.
     */
    private Map<ItemValidityStatus, List<AssessmentQuestion>> partitionByValidityStatus(
            List<AssessmentQuestion> questions) {

        Map<ItemValidityStatus, List<AssessmentQuestion>> result = new EnumMap<>(ItemValidityStatus.class);

        for (AssessmentQuestion question : questions) {
            ItemValidityStatus status = getValidityStatus(question.getId());
            result.computeIfAbsent(status, k -> new ArrayList<>()).add(question);
        }

        return result;
    }

    /**
     * Get the validity status for a question.
     * Returns PROBATION if no statistics exist yet.
     */
    private ItemValidityStatus getValidityStatus(UUID questionId) {
        return itemStatsRepository.findByQuestion_Id(questionId)
                .map(ItemStatistics::getValidityStatus)
                .orElse(ItemValidityStatus.PROBATION);
    }

    /**
     * Select questions without psychometric validation (fallback).
     */
    private List<UUID> selectWithoutValidation(List<UUID> indicatorIds, int questionsPerIndicator) {
        List<UUID> selectedQuestions = new ArrayList<>();

        for (UUID indicatorId : indicatorIds) {
            List<AssessmentQuestion> questions = questionRepository
                    .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

            Collections.shuffle(questions);
            questions.stream()
                    .limit(questionsPerIndicator)
                    .map(AssessmentQuestion::getId)
                    .forEach(selectedQuestions::add);
        }

        return selectedQuestions;
    }

    /**
     * Check if a question is eligible for test assembly.
     *
     * @param questionId The question to check
     * @return true if the question can be included in tests
     */
    public boolean isEligibleForAssembly(UUID questionId) {
        if (!psychometricsEnabled) {
            return questionRepository.findById(questionId)
                    .map(AssessmentQuestion::isActive)
                    .orElse(false);
        }

        // Check if question is active
        boolean isActive = questionRepository.findById(questionId)
                .map(AssessmentQuestion::isActive)
                .orElse(false);

        if (!isActive) {
            return false;
        }

        // Check validity status - RETIRED items are never eligible
        ItemValidityStatus status = getValidityStatus(questionId);
        return status != ItemValidityStatus.RETIRED;
    }

    /**
     * Get a summary of question availability by validity status for an indicator.
     *
     * @param indicatorId The behavioral indicator
     * @return Map of status to count
     */
    public Map<ItemValidityStatus, Integer> getAvailabilitySummary(UUID indicatorId) {
        List<AssessmentQuestion> questions = questionRepository
                .findByBehavioralIndicator_IdAndIsActiveTrue(indicatorId);

        Map<ItemValidityStatus, Integer> summary = new EnumMap<>(ItemValidityStatus.class);
        for (ItemValidityStatus status : ItemValidityStatus.values()) {
            summary.put(status, 0);
        }

        for (AssessmentQuestion question : questions) {
            ItemValidityStatus status = getValidityStatus(question.getId());
            summary.merge(status, 1, Integer::sum);
        }

        return summary;
    }

    /**
     * Check if an indicator has sufficient validated questions for test assembly.
     *
     * @param indicatorId The behavioral indicator
     * @param requiredCount Number of questions required
     * @return true if sufficient questions are available
     */
    public boolean hasSufficientQuestions(UUID indicatorId, int requiredCount) {
        Map<ItemValidityStatus, Integer> summary = getAvailabilitySummary(indicatorId);

        // Count eligible questions (ACTIVE + PROBATION + FLAGGED, but not RETIRED)
        int eligible = summary.getOrDefault(ItemValidityStatus.ACTIVE, 0)
                + summary.getOrDefault(ItemValidityStatus.PROBATION, 0)
                + summary.getOrDefault(ItemValidityStatus.FLAGGED_FOR_REVIEW, 0);

        return eligible >= requiredCount;
    }

    /**
     * Get the count of ACTIVE (validated) questions for an indicator.
     */
    public int getActiveQuestionCount(UUID indicatorId) {
        return getAvailabilitySummary(indicatorId)
                .getOrDefault(ItemValidityStatus.ACTIVE, 0);
    }

    /**
     * Get the count of questions needing data (PROBATION) for an indicator.
     */
    public int getProbationQuestionCount(UUID indicatorId) {
        return getAvailabilitySummary(indicatorId)
                .getOrDefault(ItemValidityStatus.PROBATION, 0);
    }
}
