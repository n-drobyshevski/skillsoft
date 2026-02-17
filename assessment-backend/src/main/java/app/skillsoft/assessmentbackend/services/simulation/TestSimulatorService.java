package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.*;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import app.skillsoft.assessmentbackend.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service for simulating test execution ("Dry Run").
 * 
 * Provides a way to verify test configuration before actual deployment:
 * - Assembles questions using the appropriate strategy
 * - Simulates candidate responses based on persona profiles
 * - Calculates estimated scores and duration
 * - Identifies inventory issues and configuration problems
 * 
 * This is a validation tool, not an actual test execution engine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TestSimulatorService {

    private final TestAssemblerFactory assemblerFactory;
    private final AssessmentQuestionRepository questionRepository;
    private final InventoryHeatmapService inventoryHeatmapService;

    /**
     * Default time per question in seconds (for estimation).
     */
    private static final int DEFAULT_TIME_PER_QUESTION_SECONDS = 60;

    /**
     * Simulate test execution with the given blueprint and profile.
     * 
     * @param blueprint The test blueprint configuration
     * @param profile The simulation persona (PERFECT, RANDOM, FAILING)
     * @return Simulation results with composition, sample questions, and warnings
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.SIMULATION_RESULTS_CACHE,
               key = "#blueprint.hashCode() + '_' + #profile.name()",
               condition = "#blueprint != null && #profile != null")
    public SimulationResultDto simulate(TestBlueprintDto blueprint, SimulationProfile profile) {
        if (blueprint == null) {
            return SimulationResultDto.failed(List.of(
                InventoryWarning.info("Blueprint is null")
            ));
        }

        if (profile == null) {
            profile = SimulationProfile.RANDOM_GUESSER;
        }

        log.info("Starting simulation with profile: {} for strategy: {}", 
            profile, blueprint.getStrategy());

        var warnings = new ArrayList<InventoryWarning>();

        // Step 1: Get appropriate assembler and assemble questions
        List<UUID> questionIds;
        try {
            var assembler = assemblerFactory.getAssembler(blueprint);
            questionIds = assembler.assemble(blueprint);
        } catch (IllegalArgumentException e) {
            log.error("Failed to get assembler: {}", e.getMessage());
            return SimulationResultDto.failed(List.of(
                InventoryWarning.info("Assembly failed: " + e.getMessage())
            ));
        }

        if (questionIds.isEmpty()) {
            warnings.add(InventoryWarning.info("No questions assembled - check blueprint configuration"));
            return SimulationResultDto.builder()
                .valid(false)
                .warnings(warnings)
                .totalQuestions(0)
                .profile(profile)
                .build();
        }

        log.debug("Assembled {} questions for simulation", questionIds.size());

        // Step 2: Hydrate question UUIDs to entities
        var questions = hydrateQuestions(questionIds);
        
        if (questions.size() < questionIds.size()) {
            warnings.add(InventoryWarning.info(
                String.format("Only %d of %d questions could be loaded", 
                    questions.size(), questionIds.size())
            ));
        }

        // Step 3: Check inventory health for involved competencies
        var competencyIds = extractCompetencyIds(questions);
        checkInventoryHealth(competencyIds, warnings);

        // Step 4: Run persona simulation
        var simulationRun = runPersonaSimulation(questions, profile);

        // Step 5: Calculate composition and statistics
        var composition = calculateComposition(questions);
        var estimatedDuration = calculateEstimatedDuration(questions);
        var simulatedScore = calculateSimulatedScore(simulationRun);

        // Determine validity
        var valid = warnings.stream()
            .noneMatch(w -> w.level() == InventoryWarning.WarningLevel.ERROR);

        log.info("Simulation complete: {} questions, score: {}, duration: {} min, valid: {}",
            questions.size(), simulatedScore, estimatedDuration, valid);

        return SimulationResultDto.builder()
            .valid(valid)
            .composition(composition)
            .sampleQuestions(simulationRun)
            .warnings(warnings)
            .simulatedScore(simulatedScore)
            .estimatedDurationMinutes(estimatedDuration)
            .totalQuestions(questions.size())
            .profile(profile)
            .build();
    }

    /**
     * Quick validation without full simulation.
     * Just checks if assembly would succeed and inventory is sufficient.
     */
    @Transactional(readOnly = true)
    public boolean validate(TestBlueprintDto blueprint) {
        try {
            var assembler = assemblerFactory.getAssembler(blueprint);
            var questionIds = assembler.assemble(blueprint);
            return !questionIds.isEmpty();
        } catch (Exception e) {
            log.warn("Blueprint validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Hydrate question UUIDs to full entities.
     */
    private List<AssessmentQuestion> hydrateQuestions(List<UUID> questionIds) {
        var questionsById = questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));
        
        // Preserve order from assembly
        return questionIds.stream()
            .map(questionsById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * Extract unique competency IDs from questions.
     */
    private List<UUID> extractCompetencyIds(List<AssessmentQuestion> questions) {
        return questions.stream()
            .map(q -> q.getBehavioralIndicator().getCompetency().getId())
            .distinct()
            .toList();
    }

    /**
     * Check inventory health and add warnings for issues.
     */
    private void checkInventoryHealth(List<UUID> competencyIds, List<InventoryWarning> warnings) {
        var heatmap = inventoryHeatmapService.generateHeatmapFor(competencyIds);
        
        for (var entry : heatmap.competencyHealth().entrySet()) {
            if (entry.getValue() == HealthStatus.CRITICAL) {
                warnings.add(InventoryWarning.critical(
                    entry.getKey(),
                    "Unknown", // Would need to fetch name
                    "ALL",
                    0, // Would need actual count
                    5
                ));
            } else if (entry.getValue() == HealthStatus.MODERATE) {
                warnings.add(InventoryWarning.moderate(
                    entry.getKey(),
                    "Unknown",
                    "ALL",
                    3,
                    5
                ));
            }
        }
    }

    /**
     * Run persona simulation through questions.
     */
    private List<QuestionSummaryDto> runPersonaSimulation(
            List<AssessmentQuestion> questions, 
            SimulationProfile profile
    ) {
        var results = new ArrayList<QuestionSummaryDto>();
        var random = ThreadLocalRandom.current();
        var correctProbability = profile.getCorrectAnswerProbability();

        for (var question : questions) {
            // Determine if answer is "correct" based on profile
            var isCorrect = switch (profile) {
                case PERFECT_CANDIDATE -> true;
                case FAILING_CANDIDATE -> false;
                case RANDOM_GUESSER -> random.nextDouble() < correctProbability;
            };

            var simulatedAnswer = isCorrect ? "Correct Option" : "Incorrect Option";

            var summary = QuestionSummaryDto.of(
                question.getId(),
                question.getBehavioralIndicator().getCompetency().getId(),
                question.getBehavioralIndicator().getId(),
                truncateText(question.getQuestionText(), 100),
                question.getDifficultyLevel().name(),
                question.getQuestionType().name(),
                question.getTimeLimit(),
                isCorrect,
                simulatedAnswer
            );

            results.add(summary);
        }

        return results;
    }

    /**
     * Calculate question composition by difficulty.
     */
    private Map<String, Integer> calculateComposition(List<AssessmentQuestion> questions) {
        return questions.stream()
            .collect(Collectors.groupingBy(
                q -> q.getDifficultyLevel().name(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    /**
     * Estimate total test duration in minutes.
     */
    private Integer calculateEstimatedDuration(List<AssessmentQuestion> questions) {
        var totalSeconds = questions.stream()
            .mapToInt(q -> q.getTimeLimit() != null ? q.getTimeLimit() : DEFAULT_TIME_PER_QUESTION_SECONDS)
            .sum();
        
        return (int) Math.ceil(totalSeconds / 60.0);
    }

    /**
     * Calculate simulated score from run results.
     */
    private Double calculateSimulatedScore(List<QuestionSummaryDto> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        
        var correct = results.stream()
            .filter(QuestionSummaryDto::simulatedCorrect)
            .count();
        
        return (double) correct / results.size() * 100;
    }

    /**
     * Truncate text for display.
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
