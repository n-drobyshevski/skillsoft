package app.skillsoft.assessmentbackend.services.assembly;

import app.skillsoft.assessmentbackend.domain.dto.blueprint.JobFitBlueprint;
import app.skillsoft.assessmentbackend.domain.dto.blueprint.TestBlueprintDto;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentGoal;
import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.external.OnetService;
import app.skillsoft.assessmentbackend.services.external.OnetService.OnetProfile;
import app.skillsoft.assessmentbackend.services.external.PassportService;
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
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobFitAssembler implements TestAssembler {

    private final OnetService onetService;
    private final PassportService passportService;
    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;

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

        log.info("Assembling JOB_FIT test for SOC code: {}", socCode);

        // Step 1: Fetch O*NET profile/benchmarks
        var onetProfile = onetService.getProfile(socCode);
        if (onetProfile.isEmpty()) {
            log.warn("No O*NET profile found for SOC code: {}", socCode);
            return List.of();
        }

        var benchmarks = onetProfile.get().benchmarks();
        log.debug("Found {} benchmark competencies for {}", benchmarks.size(), socCode);

        // Step 2: Calculate gaps (for now, assume no passport - full assessment)
        // In real implementation, would check passportService for existing scores
        var gapAnalysis = analyzeGaps(benchmarks, jobFitBlueprint.getStrictnessLevel());

        // Step 3: Select questions based on gap analysis
        var selectedQuestions = selectQuestionsForGaps(gapAnalysis, jobFitBlueprint.getStrictnessLevel());

        log.info("Assembled {} questions for JOB_FIT assessment (SOC: {})", 
            selectedQuestions.size(), socCode);

        return selectedQuestions;
    }

    /**
     * Analyze gaps between benchmarks and candidate scores.
     * Returns map of competency name to gap value.
     */
    private Map<String, GapInfo> analyzeGaps(Map<String, Double> benchmarks, int strictnessLevel) {
        var gaps = new HashMap<String, GapInfo>();
        
        // For now, treat all benchmarks as gaps (no passport available)
        // Adjust threshold based on strictness level
        var adjustedThreshold = SIGNIFICANT_GAP_THRESHOLD * (100.0 - strictnessLevel) / 100.0;
        
        for (var entry : benchmarks.entrySet()) {
            var competencyName = entry.getKey();
            var benchmark = entry.getValue();
            
            // Without passport data, gap equals benchmark (candidate score assumed 0)
            // In real implementation, would subtract passport score
            var gap = benchmark; // Simplified for mock
            var isSignificant = gap > adjustedThreshold;
            
            gaps.put(competencyName, new GapInfo(competencyName, benchmark, 0.0, gap, isSignificant));
        }
        
        return gaps;
    }

    /**
     * Select questions based on gap analysis.
     */
    private List<UUID> selectQuestionsForGaps(Map<String, GapInfo> gapAnalysis, int strictnessLevel) {
        var selectedQuestions = new ArrayList<UUID>();
        var usedQuestions = new HashSet<UUID>();

        // Sort gaps by significance (largest gaps first)
        var sortedGaps = gapAnalysis.values().stream()
            .sorted(Comparator.comparing(GapInfo::gap).reversed())
            .toList();

        for (var gapInfo : sortedGaps) {
            // Determine target difficulty based on gap significance
            var targetDifficulty = gapInfo.isSignificant() 
                ? DifficultyLevel.ADVANCED 
                : DifficultyLevel.INTERMEDIATE;

            // Find questions for this competency area
            var questions = findQuestionsForCompetencyName(
                gapInfo.competencyName(), 
                targetDifficulty
            );

            // Select up to DEFAULT_QUESTIONS_PER_GAP questions
            var count = 0;
            for (var questionId : questions) {
                if (!usedQuestions.contains(questionId) && count < DEFAULT_QUESTIONS_PER_GAP) {
                    selectedQuestions.add(questionId);
                    usedQuestions.add(questionId);
                    count++;
                }
            }
        }

        return selectedQuestions;
    }

    /**
     * Find questions matching a competency name and difficulty.
     * In real implementation, would use proper competency ID matching via standard codes.
     */
    private List<UUID> findQuestionsForCompetencyName(String competencyName, DifficultyLevel targetDifficulty) {
        // This is a simplified implementation
        // Real implementation would:
        // 1. Map O*NET competency name to internal competency IDs via standard_codes JSONB
        // 2. Get indicators for those competencies
        // 3. Get questions for those indicators with target difficulty
        
        return questionRepository.findAll().stream()
            .filter(q -> q.isActive())
            .filter(q -> q.getDifficultyLevel() == targetDifficulty)
            .map(AssessmentQuestion::getId)
            .limit(DEFAULT_QUESTIONS_PER_GAP)
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
