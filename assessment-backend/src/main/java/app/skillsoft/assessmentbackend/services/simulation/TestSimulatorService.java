package app.skillsoft.assessmentbackend.services.simulation;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.*;
import app.skillsoft.assessmentbackend.domain.dto.validation.BlueprintValidationResult;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.assembly.AssemblyResult;
import app.skillsoft.assessmentbackend.services.assembly.TestAssemblerFactory;
import app.skillsoft.assessmentbackend.services.validation.BlueprintValidationService;
import app.skillsoft.assessmentbackend.services.validation.InventoryHeatmapService;
import app.skillsoft.assessmentbackend.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final CompetencyRepository competencyRepository;
    private final InventoryHeatmapService inventoryHeatmapService;
    private final BlueprintValidationService blueprintValidationService;

    private static final int DEFAULT_TIME_PER_QUESTION_SECONDS = 60;

    /**
     * Simulate test execution with pre-validation against a full template.
     */
    @Transactional(readOnly = true)
    public SimulationResultDto simulateWithValidation(TestTemplate template, SimulationProfile profile, int abilityLevel) {
        if (template == null) {
            return SimulationResultDto.failed(List.of(
                InventoryWarning.info("Template is null")
            ));
        }

        BlueprintValidationResult validationResult =
                blueprintValidationService.validateForSimulation(template);

        if (!validationResult.canSimulate()) {
            log.warn("Template {} failed pre-simulation validation: {}",
                    template.getId(), validationResult.errorMessages());

            List<InventoryWarning> warnings = validationResult.errors().stream()
                    .map(issue -> InventoryWarning.info("Validation: " + issue.message()))
                    .collect(Collectors.toList());

            return SimulationResultDto.failed(warnings);
        }

        if (validationResult.hasWarnings()) {
            log.info("Template {} has {} simulation warnings",
                    template.getId(), validationResult.warnings().size());
        }

        return simulate(template.getTypedBlueprint(), profile, abilityLevel);
    }

    /**
     * Backward-compatible overload defaulting abilityLevel to 50.
     */
    @Transactional(readOnly = true)
    public SimulationResultDto simulateWithValidation(TestTemplate template, SimulationProfile profile) {
        return simulateWithValidation(template, profile, 50);
    }

    /**
     * Simulate test execution with the given blueprint, profile, and ability level.
     *
     * @param blueprint    The test blueprint configuration
     * @param profile      The simulation persona (PERFECT, RANDOM, FAILING)
     * @param abilityLevel Ability slider value (0-100); shifts the persona's response curve
     * @return Simulation results with composition, sample questions, and warnings
     */
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.SIMULATION_RESULTS_CACHE,
               key = "#blueprint.toString() + '_' + #profile.name() + '_' + #abilityLevel",
               condition = "#blueprint != null && #profile != null")
    public SimulationResultDto simulate(TestBlueprintDto blueprint, SimulationProfile profile, int abilityLevel) {
        if (blueprint == null) {
            return SimulationResultDto.failed(List.of(
                InventoryWarning.info("Blueprint is null")
            ));
        }

        if (profile == null) {
            profile = SimulationProfile.RANDOM_GUESSER;
        }

        log.info("Starting simulation with profile: {}, abilityLevel: {} for strategy: {}",
            profile, abilityLevel, blueprint.getStrategy());

        var warnings = new ArrayList<InventoryWarning>();

        // Step 1: Get appropriate assembler and assemble questions
        List<UUID> questionIds;
        try {
            var assembler = assemblerFactory.getAssembler(blueprint);
            var assemblyResult = assembler.assemble(blueprint);
            questionIds = assemblyResult.questionIds();
            warnings.addAll(assemblyResult.warnings());
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
                .abilityLevel(abilityLevel)
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

        // Step 4: Run persona simulation with psychometric curves
        var simulationRun = runPersonaSimulation(questions, profile, abilityLevel);

        // Step 5: Calculate composition and statistics
        var composition = calculateComposition(questions);
        var estimatedDuration = calculateEstimatedDuration(questions);
        var simulatedScore = calculateSimulatedScore(simulationRun);

        // Step 6: Compute per-competency simulation scores
        var competencyScores = calculateCompetencyScores(simulationRun);

        // Determine validity
        var valid = warnings.stream()
            .noneMatch(w -> w.level() == InventoryWarning.WarningLevel.ERROR);

        log.info("Simulation complete: {} questions, score: {}, abilityLevel: {}, duration: {} min, valid: {}, competencies: {}",
            questions.size(), simulatedScore, abilityLevel, estimatedDuration, valid, competencyScores.size());

        return SimulationResultDto.builder()
            .valid(valid)
            .composition(composition)
            .sampleQuestions(simulationRun)
            .warnings(warnings)
            .simulatedScore(simulatedScore)
            .estimatedDurationMinutes(estimatedDuration)
            .totalQuestions(questions.size())
            .profile(profile)
            .competencyScores(competencyScores)
            .abilityLevel(abilityLevel)
            .build();
    }

    /**
     * Backward-compatible overload defaulting abilityLevel to 50.
     */
    @Transactional(readOnly = true)
    public SimulationResultDto simulate(TestBlueprintDto blueprint, SimulationProfile profile) {
        return simulate(blueprint, profile, 50);
    }

    /**
     * Quick validation without full simulation.
     */
    @Transactional(readOnly = true)
    public boolean validate(TestBlueprintDto blueprint) {
        try {
            var assembler = assemblerFactory.getAssembler(blueprint);
            var questionIds = assembler.assemble(blueprint).questionIds();
            return !questionIds.isEmpty();
        } catch (Exception e) {
            log.warn("Blueprint validation failed: {}", e.getMessage());
            return false;
        }
    }

    private List<AssessmentQuestion> hydrateQuestions(List<UUID> questionIds) {
        var questionsById = questionRepository.findAllById(questionIds).stream()
            .collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));

        return questionIds.stream()
            .map(questionsById::get)
            .filter(Objects::nonNull)
            .toList();
    }

    private List<UUID> extractCompetencyIds(List<AssessmentQuestion> questions) {
        return questions.stream()
            .map(q -> q.getBehavioralIndicator().getCompetency().getId())
            .distinct()
            .toList();
    }

    private void checkInventoryHealth(List<UUID> competencyIds, List<InventoryWarning> warnings) {
        var heatmap = inventoryHeatmapService.generateHeatmapFor(competencyIds);

        Map<UUID, String> competencyNames = competencyRepository.findAllById(competencyIds).stream()
                .collect(Collectors.toMap(Competency::getId, Competency::getName));

        Map<UUID, Long> availableCounts = new HashMap<>();
        for (var detailEntry : heatmap.detailedCounts().entrySet()) {
            String[] parts = detailEntry.getKey().split(":");
            if (parts.length > 0) {
                try {
                    UUID compId = UUID.fromString(parts[0]);
                    availableCounts.merge(compId, detailEntry.getValue(), Long::sum);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        int recommendedPerCompetency = 5;

        for (var entry : heatmap.competencyHealth().entrySet()) {
            UUID compId = entry.getKey();
            String compName = competencyNames.getOrDefault(compId, compId.toString());
            int available = availableCounts.getOrDefault(compId, 0L).intValue();

            if (entry.getValue() == HealthStatus.CRITICAL) {
                warnings.add(InventoryWarning.critical(
                    compId, compName, "ALL", available, recommendedPerCompetency
                ));
            } else if (entry.getValue() == HealthStatus.MODERATE) {
                warnings.add(InventoryWarning.moderate(
                    compId, compName, "ALL", available, recommendedPerCompetency
                ));
            }
        }
    }

    /**
     * Run persona simulation using IRT-inspired psychometric response curves.
     *
     * <p>For each question, the probability of a correct answer is computed as:</p>
     * <ol>
     *   <li>Base probability from the persona's difficulty curve</li>
     *   <li>Ability modifier shifts the curve in logit space</li>
     *   <li>Per-competency noise adds natural inter-competency variation</li>
     *   <li>Seeded RNG ensures deterministic, cacheable results</li>
     * </ol>
     */
    private List<QuestionSummaryDto> runPersonaSimulation(
            List<AssessmentQuestion> questions,
            SimulationProfile profile,
            int abilityLevel
    ) {
        var results = new ArrayList<QuestionSummaryDto>();

        long seed = SimulationMath.computeSimulationSeed(questions, profile, abilityLevel);
        var random = new Random(seed);

        double abilityModifier = SimulationMath.abilityToModifier(abilityLevel);
        Map<UUID, Double> competencyNoise = SimulationMath.computeCompetencyNoise(questions, seed);

        for (var question : questions) {
            var difficulty = question.getDifficultyLevel();
            UUID competencyId = question.getBehavioralIndicator().getCompetency().getId();

            double baseP = profile.getBaseProbability(difficulty);
            double adjustedP = SimulationMath.applyLogitShift(baseP, abilityModifier);
            double noise = competencyNoise.getOrDefault(competencyId, 0.0);
            double finalP = SimulationMath.applyLogitShift(adjustedP, noise);

            boolean isCorrect = random.nextDouble() < finalP;
            String simulatedAnswer = isCorrect ? "Correct Option" : "Incorrect Option";

            results.add(QuestionSummaryDto.of(
                question.getId(),
                competencyId,
                question.getBehavioralIndicator().getId(),
                truncateText(question.getQuestionText(), 100),
                difficulty.name(),
                question.getQuestionType().name(),
                question.getTimeLimit(),
                isCorrect,
                simulatedAnswer,
                question.getBehavioralIndicator().getCompetency().getName(),
                question.getBehavioralIndicator().getTitle()
            ));
        }

        return results;
    }

    private Map<String, Integer> calculateComposition(List<AssessmentQuestion> questions) {
        return questions.stream()
            .collect(Collectors.groupingBy(
                q -> q.getDifficultyLevel().name(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
    }

    private Integer calculateEstimatedDuration(List<AssessmentQuestion> questions) {
        var totalSeconds = questions.stream()
            .mapToInt(q -> q.getTimeLimit() != null ? q.getTimeLimit() : DEFAULT_TIME_PER_QUESTION_SECONDS)
            .sum();

        return (int) Math.ceil(totalSeconds / 60.0);
    }

    private Map<UUID, CompetencySimulationScore> calculateCompetencyScores(
            List<QuestionSummaryDto> results) {

        if (results == null || results.isEmpty()) {
            return Map.of();
        }

        return results.stream()
            .collect(Collectors.groupingBy(QuestionSummaryDto::competencyId))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    var questions = entry.getValue();
                    int total = questions.size();
                    int correct = (int) questions.stream()
                            .filter(QuestionSummaryDto::simulatedCorrect)
                            .count();
                    var difficultyBreakdown = questions.stream()
                            .collect(Collectors.groupingBy(
                                    QuestionSummaryDto::difficulty,
                                    Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                            ));
                    return CompetencySimulationScore.of(
                            entry.getKey(), total, correct, difficultyBreakdown);
                }
            ));
    }

    private Double calculateSimulatedScore(List<QuestionSummaryDto> results) {
        if (results.isEmpty()) {
            return 0.0;
        }

        var correct = results.stream()
            .filter(QuestionSummaryDto::simulatedCorrect)
            .count();

        return (double) Math.round((double) correct / results.size() * 100);
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
