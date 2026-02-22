package app.skillsoft.assessmentbackend.services.impl;

import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.CompetencyStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.IndicatorStatsDto;
import app.skillsoft.assessmentbackend.domain.dto.stats.EntityStatsDto.QuestionStatsDto;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.BehavioralIndicatorRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import app.skillsoft.assessmentbackend.services.EntityStatsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EntityStatsServiceImpl implements EntityStatsService {

    private final CompetencyRepository competencyRepository;
    private final BehavioralIndicatorRepository indicatorRepository;
    private final AssessmentQuestionRepository questionRepository;

    public EntityStatsServiceImpl(
            CompetencyRepository competencyRepository,
            BehavioralIndicatorRepository indicatorRepository,
            AssessmentQuestionRepository questionRepository
    ) {
        this.competencyRepository = competencyRepository;
        this.indicatorRepository = indicatorRepository;
        this.questionRepository = questionRepository;
    }

    @Override
    public EntityStatsDto getEntityStats() {
        return new EntityStatsDto(
                computeCompetencyStats(),
                computeIndicatorStats(),
                computeQuestionStats()
        );
    }

    private CompetencyStatsDto computeCompetencyStats() {
        long total = competencyRepository.count();
        long active = competencyRepository.countByIsActiveTrue();
        long withIndicators = competencyRepository.countWithIndicators();
        double avgWeight = round1(competencyRepository.averageIndicatorWeight());

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : competencyRepository.countByCategory()) {
            CompetencyCategory category = (CompetencyCategory) row[0];
            Long count = (Long) row[1];
            byCategory.put(category.name(), count);
        }

        return new CompetencyStatsDto(total, active, withIndicators, avgWeight, byCategory);
    }

    private IndicatorStatsDto computeIndicatorStats() {
        long total = indicatorRepository.count();
        long active = indicatorRepository.countByIsActiveTrue();
        long withQuestions = indicatorRepository.countWithActiveQuestions();

        List<IndicatorMeasurementType> measurableTypes = List.of(
                IndicatorMeasurementType.FREQUENCY,
                IndicatorMeasurementType.QUALITY,
                IndicatorMeasurementType.IMPACT
        );
        long measurable = indicatorRepository.countByMeasurementTypeIn(measurableTypes);
        double avgComplexity = round1(indicatorRepository.averageObservabilityComplexity());

        Map<String, Long> byContextScope = new LinkedHashMap<>();
        for (ContextScope scope : ContextScope.values()) {
            long count = indicatorRepository.countByContextScope(scope);
            byContextScope.put(scope.name(), count);
        }

        return new IndicatorStatsDto(total, active, withQuestions, measurable, avgComplexity, byContextScope);
    }

    private QuestionStatsDto computeQuestionStats() {
        long total = questionRepository.count();
        long active = questionRepository.countByIsActiveTrue();
        long withActiveIndicators = questionRepository.countWithActiveIndicators();

        List<DifficultyLevel> hardLevels = List.of(
                DifficultyLevel.ADVANCED,
                DifficultyLevel.EXPERT,
                DifficultyLevel.SPECIALIZED
        );
        long hardQuestions = questionRepository.countByDifficultyLevelIn(hardLevels);
        double avgTimeLimit = round1(questionRepository.averageTimeLimit());

        Map<String, Long> byDifficulty = new LinkedHashMap<>();
        for (DifficultyLevel level : DifficultyLevel.values()) {
            byDifficulty.put(level.name(), questionRepository.countByDifficultyLevel(level));
        }

        Map<String, Long> byQuestionType = new LinkedHashMap<>();
        for (QuestionType type : QuestionType.values()) {
            long count = questionRepository.countByQuestionType(type);
            if (count > 0) {
                byQuestionType.put(type.name(), count);
            }
        }

        return new QuestionStatsDto(total, active, withActiveIndicators, hardQuestions, avgTimeLimit, byDifficulty, byQuestionType);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
