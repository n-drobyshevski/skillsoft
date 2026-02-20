package app.skillsoft.assessmentbackend.services.diagnostics;

import app.skillsoft.assessmentbackend.domain.entities.AssessmentQuestion;
import app.skillsoft.assessmentbackend.domain.entities.BehavioralIndicator;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.TestTemplate;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.repository.TestTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating template diagnostics.
 *
 * Extracted from TestSessionController to:
 * - Separate business logic from HTTP handling
 * - Enable batch-loading of competencies, indicators, and questions
 * - Reduce repository dependencies in the controller
 */
@Service
@Transactional(readOnly = true)
public class TemplateDiagnosticsService {

    private static final Logger log = LoggerFactory.getLogger(TemplateDiagnosticsService.class);

    private final TestTemplateRepository templateRepository;
    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;

    public TemplateDiagnosticsService(
            TestTemplateRepository templateRepository,
            CompetencyRepository competencyRepository,
            BehavioralIndicatorRepository indicatorRepository,
            AssessmentQuestionRepository questionRepository) {
        this.templateRepository = templateRepository;
        this.competencyRepository = competencyRepository;
        this.indicatorRepository = indicatorRepository;
        this.questionRepository = questionRepository;
    }

    /**
     * Generate detailed diagnostics for question availability in a template.
     *
     * @param templateId Template UUID to diagnose
     * @return Optional containing diagnostics map, or empty if template not found
     */
    public Optional<Map<String, Object>> generateDiagnostics(UUID templateId) {
        Optional<TestTemplate> templateOpt = templateRepository.findById(templateId);
        if (templateOpt.isEmpty()) {
            return Optional.empty();
        }

        TestTemplate template = templateOpt.get();
        Map<String, Object> diagnostics = new LinkedHashMap<>();

        diagnostics.put("templateId", templateId);
        diagnostics.put("templateName", template.getName());
        diagnostics.put("goal", template.getGoal());
        diagnostics.put("questionsPerIndicator", template.getQuestionsPerIndicator());
        diagnostics.put("isActive", template.getIsActive());

        List<UUID> competencyIds = template.getCompetencyIds();
        diagnostics.put("competencyCount", competencyIds != null ? competencyIds.size() : 0);

        List<Map<String, Object>> competencyDiagnostics = new ArrayList<>();
        int totalAvailableQuestions = 0;
        int totalRequiredQuestions = 0;

        if (competencyIds != null && !competencyIds.isEmpty()) {
            // Batch-load all competencies in one query
            Map<UUID, Competency> competencyMap = competencyRepository
                    .findAllById(competencyIds).stream()
                    .collect(Collectors.toMap(Competency::getId, c -> c));

            // Batch-load all indicators for all competencies in one query
            Map<UUID, List<BehavioralIndicator>> indicatorsByCompetency = indicatorRepository
                    .findByCompetencyIdIn(new HashSet<>(competencyIds)).stream()
                    .collect(Collectors.groupingBy(ind -> ind.getCompetency().getId()));

            // Batch-load all questions for all indicators in one query
            Set<UUID> allIndicatorIds = indicatorsByCompetency.values().stream()
                    .flatMap(List::stream)
                    .map(BehavioralIndicator::getId)
                    .collect(Collectors.toSet());

            Map<UUID, List<AssessmentQuestion>> questionsByIndicator = Collections.emptyMap();
            if (!allIndicatorIds.isEmpty()) {
                questionsByIndicator = questionRepository
                        .findByBehavioralIndicator_IdIn(allIndicatorIds).stream()
                        .collect(Collectors.groupingBy(q -> q.getBehavioralIndicator().getId()));
            }

            for (UUID compId : competencyIds) {
                Map<String, Object> compDiag = new LinkedHashMap<>();
                compDiag.put("competencyId", compId);

                String compName = Optional.ofNullable(competencyMap.get(compId))
                        .map(Competency::getName)
                        .orElse("Unknown");
                compDiag.put("competencyName", compName);

                List<BehavioralIndicator> indicators =
                        indicatorsByCompetency.getOrDefault(compId, List.of());
                compDiag.put("indicatorCount", indicators.size());

                // Build indicator details from batch-loaded data
                List<Map<String, Object>> indicatorDetails = new ArrayList<>();
                long activeQuestionTotal = 0;

                for (BehavioralIndicator ind : indicators) {
                    Map<String, Object> indDetail = new LinkedHashMap<>();
                    indDetail.put("id", ind.getId());
                    indDetail.put("title", ind.getTitle());
                    indDetail.put("contextScope",
                            ind.getContextScope() != null ? ind.getContextScope().name() : "NULL");
                    indDetail.put("isActive", ind.isActive());

                    List<AssessmentQuestion> questionsForInd =
                            questionsByIndicator.getOrDefault(ind.getId(), List.of());
                    long activeCount = questionsForInd.stream()
                            .filter(AssessmentQuestion::isActive)
                            .count();
                    indDetail.put("activeQuestionCount", activeCount);
                    activeQuestionTotal += activeCount;

                    indicatorDetails.add(indDetail);
                }
                compDiag.put("indicators", indicatorDetails);
                compDiag.put("activeQuestionCount", activeQuestionTotal);

                // Calculate shortfall per indicator (questionsPerIndicator is the
                // number of questions needed per individual indicator, not per competency)
                int questionsPerInd = template.getQuestionsPerIndicator();
                int compShortfall = 0;
                int compRequired = 0;
                int compAvailable = 0;

                for (BehavioralIndicator ind : indicators) {
                    List<AssessmentQuestion> questionsForInd =
                            questionsByIndicator.getOrDefault(ind.getId(), List.of());
                    int activeForIndicator = (int) questionsForInd.stream()
                            .filter(AssessmentQuestion::isActive)
                            .count();
                    compRequired += questionsPerInd;
                    compAvailable += activeForIndicator;
                    compShortfall += Math.max(0, questionsPerInd - activeForIndicator);
                }

                // If competency has no indicators, shortfall is the full requirement
                if (indicators.isEmpty()) {
                    compRequired = questionsPerInd;
                    compShortfall = questionsPerInd;
                }

                compDiag.put("questionsRequired", compRequired);
                compDiag.put("questionsAvailable", compAvailable);
                compDiag.put("shortfall", compShortfall);

                totalAvailableQuestions += compAvailable;
                totalRequiredQuestions += compRequired;

                competencyDiagnostics.add(compDiag);
            }
        }

        diagnostics.put("competencies", competencyDiagnostics);
        diagnostics.put("totalQuestionsAvailable", totalAvailableQuestions);
        diagnostics.put("totalQuestionsRequired", totalRequiredQuestions);
        diagnostics.put("canStartSession", totalAvailableQuestions >= totalRequiredQuestions);

        // Add troubleshooting tips based on findings
        List<String> issues = new ArrayList<>();
        for (Map<String, Object> compDiag : competencyDiagnostics) {
            int shortfall = (int) compDiag.get("shortfall");
            if (shortfall > 0) {
                issues.add(String.format("Competency '%s' needs %d more questions",
                        compDiag.get("competencyName"), shortfall));
            }
            int indicatorCount = (int) compDiag.get("indicatorCount");
            if (indicatorCount == 0) {
                issues.add(String.format("Competency '%s' has no behavioral indicators",
                        compDiag.get("competencyName")));
            }
        }
        diagnostics.put("issues", issues);

        if (!issues.isEmpty()) {
            List<String> tips = new ArrayList<>();
            tips.add("1. Ensure each competency has at least one behavioral indicator");
            tips.add("2. Ensure each behavioral indicator has active questions (is_active=true)");
            tips.add("3. For Scenario A (OVERVIEW), ensure indicators have context_scope='UNIVERSAL' or NULL");
            tips.add("4. For optimal filtering, add 'GENERAL' tag to question metadata.tags");
            diagnostics.put("troubleshootingTips", tips);
        }

        log.info("Template {} diagnostics: {} available, {} required, canStart={}",
                templateId, totalAvailableQuestions, totalRequiredQuestions,
                totalAvailableQuestions >= totalRequiredQuestions);

        return Optional.of(diagnostics);
    }
}
