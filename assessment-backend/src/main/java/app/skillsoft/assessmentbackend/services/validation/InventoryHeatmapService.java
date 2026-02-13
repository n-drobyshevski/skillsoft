package app.skillsoft.assessmentbackend.services.validation;

import app.skillsoft.assessmentbackend.domain.dto.simulation.HealthStatus;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryHeatmapDto.HeatmapSummary;
import app.skillsoft.assessmentbackend.domain.entities.Competency;
import app.skillsoft.assessmentbackend.domain.entities.DifficultyLevel;
import app.skillsoft.assessmentbackend.repository.AssessmentQuestionRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating inventory heatmaps showing question availability health.
 * 
 * Uses efficient single-query approach to count questions grouped by
 * competency and difficulty, then calculates health status for each.
 * 
 * Health Status Thresholds:
 * - CRITICAL: < 3 questions
 * - MODERATE: 3-5 questions
 * - HEALTHY: > 5 questions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryHeatmapService {

    private final AssessmentQuestionRepository questionRepository;
    private final CompetencyRepository competencyRepository;

    /**
     * Generate a complete inventory heatmap for all competencies.
     * 
     * @return Heatmap with health status per competency and detailed breakdown
     */
    @Transactional(readOnly = true)
    public InventoryHeatmapDto generateHeatmap() {
        log.info("Generating inventory heatmap for all competencies");

        // Single efficient query to get all counts
        var rawCounts = questionRepository.countQuestionsByCompetencyAndDifficulty();
        
        // Process into detailed counts map
        var detailedCounts = processDetailedCounts(rawCounts);
        
        // Calculate per-competency health (aggregate across all difficulties)
        var competencyHealth = calculateCompetencyHealth(rawCounts);
        
        // Build summary statistics
        var summary = buildSummary(competencyHealth, detailedCounts);

        log.info("Generated heatmap: {} competencies, {} healthy, {} critical",
            summary.totalCompetencies(), summary.healthyCount(), summary.criticalCount());

        return InventoryHeatmapDto.builder()
            .competencyHealth(competencyHealth)
            .detailedCounts(detailedCounts)
            .summary(summary)
            .build();
    }

    /**
     * Generate heatmap for specific competencies.
     * 
     * @param competencyIds The competencies to analyze
     * @return Filtered heatmap
     */
    @Transactional(readOnly = true)
    public InventoryHeatmapDto generateHeatmapFor(List<UUID> competencyIds) {
        if (competencyIds == null || competencyIds.isEmpty()) {
            return generateHeatmap();
        }

        log.info("Generating inventory heatmap for {} competencies", competencyIds.size());

        // Get full heatmap and filter
        var fullHeatmap = generateHeatmap();
        
        var filteredHealth = fullHeatmap.competencyHealth().entrySet().stream()
            .filter(e -> competencyIds.contains(e.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        
        var filteredCounts = fullHeatmap.detailedCounts().entrySet().stream()
            .filter(e -> {
                var parts = e.getKey().split(":");
                if (parts.length > 0) {
                    try {
                        var compId = UUID.fromString(parts[0]);
                        return competencyIds.contains(compId);
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                }
                return false;
            })
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        var summary = buildSummary(filteredHealth, filteredCounts);

        return InventoryHeatmapDto.builder()
            .competencyHealth(filteredHealth)
            .detailedCounts(filteredCounts)
            .summary(summary)
            .build();
    }

    /**
     * Get health status for a specific competency.
     * 
     * @param competencyId The competency to check
     * @return Health status based on total question count
     */
    @Transactional(readOnly = true)
    public HealthStatus getHealthForCompetency(UUID competencyId) {
        var counts = questionRepository.countQuestionsByDifficultyForCompetency(competencyId);
        
        var totalCount = counts.stream()
            .mapToLong(row -> (Long) row[1])
            .sum();
        
        return calculateHealth(totalCount);
    }

    /**
     * Check if there are sufficient questions for a test configuration.
     * 
     * @param competencyIds Competencies to assess
     * @param questionsPerCompetency Required questions per competency
     * @return Map of competency ID to shortage amount (0 = sufficient)
     */
    @Transactional(readOnly = true)
    public Map<UUID, Integer> checkSufficiency(List<UUID> competencyIds, int questionsPerCompetency) {
        var result = new HashMap<UUID, Integer>();
        
        var heatmap = generateHeatmapFor(competencyIds);
        
        for (var compId : competencyIds) {
            var totalForComp = heatmap.detailedCounts().entrySet().stream()
                .filter(e -> e.getKey().startsWith(compId.toString()))
                .mapToLong(Map.Entry::getValue)
                .sum();
            
            var shortage = questionsPerCompetency - (int) totalForComp;
            result.put(compId, Math.max(0, shortage));
        }
        
        return result;
    }

    /**
     * Process raw query results into detailed counts map.
     * Key format: "competencyId:difficulty"
     */
    private Map<String, Long> processDetailedCounts(List<Object[]> rawCounts) {
        var result = new HashMap<String, Long>();
        
        for (var row : rawCounts) {
            var competencyId = (UUID) row[0];
            var difficulty = (DifficultyLevel) row[1];
            var count = (Long) row[2];
            
            var key = competencyId.toString() + ":" + difficulty.name();
            result.put(key, count);
        }
        
        return result;
    }

    /**
     * Calculate health status for each competency based on total questions.
     */
    private Map<UUID, HealthStatus> calculateCompetencyHealth(List<Object[]> rawCounts) {
        // First, aggregate counts per competency
        var competencyTotals = new HashMap<UUID, Long>();
        
        for (var row : rawCounts) {
            var competencyId = (UUID) row[0];
            var count = (Long) row[2];
            
            competencyTotals.merge(competencyId, count, Long::sum);
        }
        
        // Then calculate health for each
        return competencyTotals.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> calculateHealth(e.getValue())
            ));
    }

    /**
     * Calculate health status from question count.
     */
    private HealthStatus calculateHealth(long count) {
        return HealthStatus.fromCount(count);
    }

    /**
     * Build summary statistics from health map.
     */
    private HeatmapSummary buildSummary(
            Map<UUID, HealthStatus> competencyHealth,
            Map<String, Long> detailedCounts
    ) {
        var criticalCount = 0;
        var moderateCount = 0;
        var healthyCount = 0;
        
        for (var status : competencyHealth.values()) {
            switch (status) {
                case CRITICAL -> criticalCount++;
                case MODERATE -> moderateCount++;
                case HEALTHY -> healthyCount++;
            }
        }
        
        var totalQuestions = detailedCounts.values().stream()
            .mapToLong(Long::longValue)
            .sum();
        
        return new HeatmapSummary(
            competencyHealth.size(),
            criticalCount,
            moderateCount,
            healthyCount,
            totalQuestions
        );
    }
}
