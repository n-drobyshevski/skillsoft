package app.skillsoft.assessmentbackend.controller;

import app.skillsoft.assessmentbackend.domain.dto.psychometrics.*;
import app.skillsoft.assessmentbackend.domain.entities.*;
import app.skillsoft.assessmentbackend.repository.BigFiveReliabilityRepository;
import app.skillsoft.assessmentbackend.repository.CompetencyReliabilityRepository;
import app.skillsoft.assessmentbackend.repository.ItemStatisticsRepository;
import app.skillsoft.assessmentbackend.services.psychometrics.PsychometricAnalysisService;
import app.skillsoft.assessmentbackend.services.psychometrics.PsychometricAuditJob;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for Psychometric Analysis management.
 * <p>
 * Provides endpoints for viewing and managing psychometric metrics including:
 * - Item-level statistics (difficulty, discrimination)
 * - Competency-level reliability (Cronbach's Alpha)
 * - Big Five trait reliability
 * - Flagged items requiring review
 * - Manual audit triggers
 * <p>
 * Security:
 * - All endpoints require ADMIN or HR role
 * <p>
 * API Base Path: /api/v1/psychometrics
 */
@RestController
@RequestMapping("/api/v1/psychometrics")
@PreAuthorize("hasAnyRole('ADMIN', 'HR')")
public class PsychometricController {

    private static final Logger logger = LoggerFactory.getLogger(PsychometricController.class);

    private final PsychometricAnalysisService analysisService;
    private final PsychometricAuditJob auditJob;
    private final ItemStatisticsRepository itemStatsRepository;
    private final CompetencyReliabilityRepository competencyReliabilityRepository;
    private final BigFiveReliabilityRepository bigFiveReliabilityRepository;

    public PsychometricController(
            PsychometricAnalysisService analysisService,
            PsychometricAuditJob auditJob,
            ItemStatisticsRepository itemStatsRepository,
            CompetencyReliabilityRepository competencyReliabilityRepository,
            BigFiveReliabilityRepository bigFiveReliabilityRepository) {
        this.analysisService = analysisService;
        this.auditJob = auditJob;
        this.itemStatsRepository = itemStatsRepository;
        this.competencyReliabilityRepository = competencyReliabilityRepository;
        this.bigFiveReliabilityRepository = bigFiveReliabilityRepository;
    }

    // ==================== DASHBOARD ====================

    /**
     * Get psychometric health dashboard overview.
     * <p>
     * Returns comprehensive summary of psychometric health including:
     * - Item status distribution
     * - Competency reliability distribution
     * - Top flagged items
     * - Big Five reliability summary
     *
     * @return PsychometricHealthReport with aggregated metrics
     */
    @GetMapping("/dashboard")
    public ResponseEntity<PsychometricHealthReport> getDashboard() {
        logger.info("GET /api/v1/psychometrics/dashboard - Getting health report");

        PsychometricHealthReport report = analysisService.generateHealthReport();
        logger.info("Generated health report: {} items, {} competencies",
                report.totalItems(), report.totalCompetencies());

        return ResponseEntity.ok(report);
    }

    // ==================== ITEM STATISTICS ====================

    /**
     * List item statistics with pagination and filtering.
     *
     * @param pageable     pagination parameters
     * @param status       optional filter by validity status
     * @param competencyId optional filter by competency
     * @return Page of ItemStatisticsDto
     */
    @GetMapping("/items")
    public ResponseEntity<Page<ItemStatisticsDto>> listItemStatistics(
            @PageableDefault(size = 20, sort = "lastCalculatedAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) ItemValidityStatus status,
            @RequestParam(required = false) UUID competencyId) {

        logger.info("GET /api/v1/psychometrics/items - status: {}, competencyId: {}, page: {}",
                status, competencyId, pageable.getPageNumber());

        Page<ItemStatistics> statsPage;

        if (status != null) {
            statsPage = itemStatsRepository.findByValidityStatus(status, pageable);
        } else {
            statsPage = itemStatsRepository.findAll(pageable);
        }

        // Filter by competency if specified (post-query filter)
        Page<ItemStatisticsDto> result = statsPage.map(this::mapToItemStatisticsDto);

        if (competencyId != null) {
            // For competency filtering, we need to reload with the competency filter
            List<ItemStatistics> competencyStats = itemStatsRepository.findByCompetencyId(competencyId);
            Set<UUID> competencyQuestionIds = competencyStats.stream()
                    .map(ItemStatistics::getQuestionId)
                    .collect(Collectors.toSet());

            result = result.map(dto -> competencyQuestionIds.contains(dto.questionId()) ? dto : null);
        }

        logger.info("Found {} item statistics", result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    /**
     * Get detailed statistics for a specific item.
     *
     * @param questionId the question UUID
     * @return ItemStatisticsDetailDto with full details
     */
    @GetMapping("/items/{questionId}")
    public ResponseEntity<ItemStatisticsDetailDto> getItemDetail(@PathVariable UUID questionId) {
        logger.info("GET /api/v1/psychometrics/items/{}", questionId);

        return itemStatsRepository.findByQuestion_Id(questionId)
                .map(stats -> {
                    ItemStatisticsDetailDto detail = mapToItemStatisticsDetailDto(stats);
                    logger.info("Found item statistics for question: {}", questionId);
                    return ResponseEntity.ok(detail);
                })
                .orElseGet(() -> {
                    logger.warn("Item statistics not found for question: {}", questionId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Manually trigger recalculation for a specific item.
     *
     * @param questionId the question UUID to recalculate
     * @return Updated ItemStatisticsDetailDto
     */
    @PostMapping("/items/{questionId}/recalculate")
    public ResponseEntity<ItemStatisticsDetailDto> recalculateItem(@PathVariable UUID questionId) {
        logger.info("POST /api/v1/psychometrics/items/{}/recalculate", questionId);

        try {
            ItemStatistics stats = analysisService.calculateItemStatistics(questionId);
            analysisService.updateItemValidityStatus(questionId);

            ItemStatisticsDetailDto detail = mapToItemStatisticsDetailDto(stats);
            logger.info("Recalculated statistics for question: {}", questionId);
            return ResponseEntity.ok(detail);
        } catch (IllegalArgumentException e) {
            logger.warn("Question not found for recalculation: {}", questionId);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Manually override an item's validity status.
     *
     * @param questionId the question UUID
     * @param request    status update request with new status and reason
     * @return Updated ItemStatisticsDetailDto
     */
    @PutMapping("/items/{questionId}/status")
    public ResponseEntity<ItemStatisticsDetailDto> updateItemStatus(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateItemStatusRequest request) {
        logger.info("PUT /api/v1/psychometrics/items/{}/status - new status: {}", questionId, request.newStatus());

        return itemStatsRepository.findByQuestion_Id(questionId)
                .map(stats -> {
                    // Handle based on request type
                    if (request.isRetirement()) {
                        analysisService.retireItem(questionId, request.reason());
                    } else if (request.isActivation()) {
                        try {
                            analysisService.activateItem(questionId);
                        } catch (IllegalStateException e) {
                            logger.warn("Cannot activate item {}: {}", questionId, e.getMessage());
                            return ResponseEntity.badRequest().<ItemStatisticsDetailDto>build();
                        }
                    } else {
                        // For other status changes, update directly with audit trail
                        ItemValidityStatus oldStatus = stats.getValidityStatus();
                        stats.setValidityStatus(request.newStatus());
                        stats.addStatusChange(oldStatus, request.newStatus(),
                                java.time.LocalDateTime.now(), request.reason());
                        itemStatsRepository.save(stats);
                    }

                    // Reload and return updated stats
                    ItemStatistics updated = itemStatsRepository.findByQuestion_Id(questionId).orElse(stats);
                    ItemStatisticsDetailDto detail = mapToItemStatisticsDetailDto(updated);
                    logger.info("Updated status for question {} to {}", questionId, request.newStatus());
                    return ResponseEntity.ok(detail);
                })
                .orElseGet(() -> {
                    logger.warn("Item statistics not found for question: {}", questionId);
                    return ResponseEntity.notFound().build();
                });
    }

    // ==================== COMPETENCY RELIABILITY ====================

    /**
     * List competency reliability statistics.
     *
     * @param pageable pagination parameters
     * @param status   optional filter by reliability status
     * @return Page of CompetencyReliabilityDto
     */
    @GetMapping("/competencies")
    public ResponseEntity<Page<CompetencyReliabilityDto>> listCompetencyReliability(
            @PageableDefault(size = 20, sort = "cronbachAlpha", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) ReliabilityStatus status) {

        logger.info("GET /api/v1/psychometrics/competencies - status: {}", status);

        Page<CompetencyReliability> page;
        if (status != null) {
            // Use a specification or custom query for status filtering with pagination
            List<CompetencyReliability> filtered = competencyReliabilityRepository.findByReliabilityStatus(status);
            page = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        } else {
            page = competencyReliabilityRepository.findAll(pageable);
        }

        Page<CompetencyReliabilityDto> result = page.map(this::mapToCompetencyReliabilityDto);
        logger.info("Found {} competency reliability records", result.getTotalElements());

        return ResponseEntity.ok(result);
    }

    /**
     * Get detailed reliability for a specific competency.
     *
     * @param competencyId the competency UUID
     * @return CompetencyReliabilityDetailDto with full details
     */
    @GetMapping("/competencies/{competencyId}")
    public ResponseEntity<CompetencyReliabilityDetailDto> getCompetencyReliabilityDetail(
            @PathVariable UUID competencyId) {
        logger.info("GET /api/v1/psychometrics/competencies/{}", competencyId);

        return competencyReliabilityRepository.findByCompetency_Id(competencyId)
                .map(reliability -> {
                    CompetencyReliabilityDetailDto detail = mapToCompetencyReliabilityDetailDto(reliability);
                    logger.info("Found reliability for competency: {}", competencyId);
                    return ResponseEntity.ok(detail);
                })
                .orElseGet(() -> {
                    logger.warn("Reliability not found for competency: {}", competencyId);
                    return ResponseEntity.notFound().build();
                });
    }

    // ==================== FLAGGED ITEMS ====================

    /**
     * Get all items flagged for review.
     *
     * @return List of FlaggedItemSummary sorted by severity
     */
    @GetMapping("/flagged")
    public ResponseEntity<List<FlaggedItemSummary>> getFlaggedItems() {
        logger.info("GET /api/v1/psychometrics/flagged");

        List<ItemStatistics> flaggedStats = analysisService.getItemsRequiringReview();

        List<FlaggedItemSummary> summaries = flaggedStats.stream()
                .map(this::mapToFlaggedItemSummary)
                .sorted(Comparator.comparingInt(FlaggedItemSummary::getSeverityLevel).reversed())
                .collect(Collectors.toList());

        logger.info("Found {} flagged items", summaries.size());
        return ResponseEntity.ok(summaries);
    }

    // ==================== BIG FIVE RELIABILITY ====================

    /**
     * Get Big Five trait reliability statistics.
     *
     * @return List of BigFiveReliabilityDto for all traits
     */
    @GetMapping("/big-five")
    public ResponseEntity<List<BigFiveReliabilityDto>> getBigFiveReliability() {
        logger.info("GET /api/v1/psychometrics/big-five");

        List<BigFiveReliability> reliabilities = bigFiveReliabilityRepository.findAllOrderedByAlphaDesc();

        // Ensure all 5 traits are represented
        Set<BigFiveTrait> existingTraits = reliabilities.stream()
                .map(BigFiveReliability::getTrait)
                .collect(Collectors.toSet());

        List<BigFiveReliabilityDto> result = new ArrayList<>();

        // Add existing reliabilities
        for (BigFiveReliability rel : reliabilities) {
            result.add(mapToBigFiveReliabilityDto(rel));
        }

        // Add placeholder for missing traits
        for (BigFiveTrait trait : BigFiveTrait.values()) {
            if (!existingTraits.contains(trait)) {
                result.add(BigFiveReliabilityDto.builder()
                        .trait(trait)
                        .traitDisplayName(trait.getDisplayName())
                        .reliabilityStatus(ReliabilityStatus.INSUFFICIENT_DATA)
                        .build());
            }
        }

        logger.info("Returning Big Five reliability for {} traits", result.size());
        return ResponseEntity.ok(result);
    }

    // ==================== AUDIT OPERATIONS ====================

    /**
     * Manually trigger a full psychometric audit.
     *
     * @return AuditResult with summary of the audit
     */
    @PostMapping("/audit/trigger")
    public ResponseEntity<PsychometricAuditJob.AuditResult> triggerAudit() {
        logger.info("POST /api/v1/psychometrics/audit/trigger - Manual audit triggered");

        PsychometricAuditJob.AuditResult result = auditJob.triggerManualAudit();

        logger.info("Audit completed: {} items, {} competencies, {} traits recalculated",
                result.itemsRecalculated(), result.competenciesRecalculated(), result.traitsRecalculated());

        return ResponseEntity.ok(result);
    }

    // ==================== MAPPING METHODS ====================

    private ItemStatisticsDto mapToItemStatisticsDto(ItemStatistics stats) {
        AssessmentQuestion question = stats.getQuestion();
        BehavioralIndicator indicator = question != null ? question.getBehavioralIndicator() : null;
        Competency competency = indicator != null ? indicator.getCompetency() : null;

        return ItemStatisticsDto.builder()
                .id(stats.getId())
                .questionId(stats.getQuestionId())
                .questionText(question != null ? truncateText(question.getQuestionText(), 100) : null)
                .competencyName(competency != null ? competency.getName() : null)
                .indicatorTitle(indicator != null ? indicator.getTitle() : null)
                .difficultyIndex(stats.getDifficultyIndex())
                .discriminationIndex(stats.getDiscriminationIndex())
                .responseCount(stats.getResponseCount())
                .validityStatus(stats.getValidityStatus())
                .difficultyFlag(stats.getDifficultyFlag())
                .discriminationFlag(stats.getDiscriminationFlag())
                .lastCalculatedAt(stats.getLastCalculatedAt())
                .build();
    }

    private ItemStatisticsDetailDto mapToItemStatisticsDetailDto(ItemStatistics stats) {
        AssessmentQuestion question = stats.getQuestion();
        BehavioralIndicator indicator = question != null ? question.getBehavioralIndicator() : null;
        Competency competency = indicator != null ? indicator.getCompetency() : null;

        // Build status change history
        List<ItemStatisticsDetailDto.StatusChangeEntry> history = new ArrayList<>();
        if (stats.getStatusChangeHistory() != null) {
            for (ItemStatistics.StatusChangeRecord record : stats.getStatusChangeHistory()) {
                history.add(new ItemStatisticsDetailDto.StatusChangeEntry(
                        record.fromStatus(), record.toStatus(), record.timestamp(), record.reason()));
            }
        }

        // Generate recommendations based on flags
        List<String> recommendations = generateRecommendations(stats);

        return ItemStatisticsDetailDto.builder()
                .id(stats.getId())
                .questionId(stats.getQuestionId())
                .questionText(question != null ? question.getQuestionText() : null)
                .questionType(question != null ? question.getQuestionType() : null)
                .competencyId(competency != null ? competency.getId() : null)
                .competencyName(competency != null ? competency.getName() : null)
                .indicatorId(indicator != null ? indicator.getId() : null)
                .indicatorTitle(indicator != null ? indicator.getTitle() : null)
                .difficultyIndex(stats.getDifficultyIndex())
                .discriminationIndex(stats.getDiscriminationIndex())
                .previousDiscriminationIndex(stats.getPreviousDiscriminationIndex())
                .responseCount(stats.getResponseCount())
                .validityStatus(stats.getValidityStatus())
                .difficultyFlag(stats.getDifficultyFlag())
                .discriminationFlag(stats.getDiscriminationFlag())
                .distractorEfficiency(stats.getDistractorEfficiency())
                .statusChangeHistory(history)
                .recommendations(recommendations)
                .lastCalculatedAt(stats.getLastCalculatedAt())
                .build();
    }

    private CompetencyReliabilityDto mapToCompetencyReliabilityDto(CompetencyReliability reliability) {
        Competency competency = reliability.getCompetency();

        return CompetencyReliabilityDto.builder()
                .id(reliability.getId())
                .competencyId(reliability.getCompetencyId())
                .competencyName(competency != null ? competency.getName() : null)
                .cronbachAlpha(reliability.getCronbachAlpha())
                .sampleSize(reliability.getSampleSize())
                .itemCount(reliability.getItemCount())
                .reliabilityStatus(reliability.getReliabilityStatus())
                .lastCalculatedAt(reliability.getLastCalculatedAt())
                .build();
    }

    private CompetencyReliabilityDetailDto mapToCompetencyReliabilityDetailDto(CompetencyReliability reliability) {
        Competency competency = reliability.getCompetency();

        // Build alpha-if-deleted map with question info
        Map<UUID, CompetencyReliabilityDetailDto.AlphaIfDeletedEntry> alphaIfDeletedMap = new HashMap<>();
        List<CompetencyReliabilityDetailDto.ItemLoweringAlpha> itemsLoweringAlpha = new ArrayList<>();

        if (reliability.getAlphaIfDeleted() != null && reliability.getCronbachAlpha() != null) {
            BigDecimal currentAlpha = reliability.getCronbachAlpha();

            for (Map.Entry<UUID, BigDecimal> entry : reliability.getAlphaIfDeleted().entrySet()) {
                UUID questionId = entry.getKey();
                BigDecimal alphaIfDeleted = entry.getValue();

                // Get question text (would need repository access for full implementation)
                String questionText = "Question " + questionId.toString().substring(0, 8);

                BigDecimal contribution = currentAlpha.subtract(alphaIfDeleted);
                CompetencyReliabilityDetailDto.AlphaIfDeletedEntry alphaEntry =
                        new CompetencyReliabilityDetailDto.AlphaIfDeletedEntry(
                                questionId, questionText, alphaIfDeleted, contribution);
                alphaIfDeletedMap.put(questionId, alphaEntry);

                // Check if removing would improve alpha
                if (alphaIfDeleted.compareTo(currentAlpha) > 0) {
                    BigDecimal improvement = alphaIfDeleted.subtract(currentAlpha);
                    String recommendation = generateItemRecommendation(improvement);
                    itemsLoweringAlpha.add(new CompetencyReliabilityDetailDto.ItemLoweringAlpha(
                            questionId, questionText, alphaIfDeleted, improvement, recommendation));
                }
            }

            // Sort by improvement amount (descending)
            itemsLoweringAlpha.sort((a, b) -> b.improvementAmount().compareTo(a.improvementAmount()));
        }

        return CompetencyReliabilityDetailDto.builder()
                .id(reliability.getId())
                .competencyId(reliability.getCompetencyId())
                .competencyName(competency != null ? competency.getName() : null)
                .competencyCategory(competency != null && competency.getCategory() != null
                        ? competency.getCategory().name() : null)
                .cronbachAlpha(reliability.getCronbachAlpha())
                .sampleSize(reliability.getSampleSize())
                .itemCount(reliability.getItemCount())
                .reliabilityStatus(reliability.getReliabilityStatus())
                .alphaIfDeleted(alphaIfDeletedMap)
                .itemsLoweringAlpha(itemsLoweringAlpha)
                .lastCalculatedAt(reliability.getLastCalculatedAt())
                .build();
    }

    private BigFiveReliabilityDto mapToBigFiveReliabilityDto(BigFiveReliability reliability) {
        return BigFiveReliabilityDto.builder()
                .id(reliability.getId())
                .trait(reliability.getTrait())
                .traitDisplayName(reliability.getTrait() != null ? reliability.getTrait().getDisplayName() : null)
                .cronbachAlpha(reliability.getCronbachAlpha())
                .contributingCompetencies(reliability.getContributingCompetencies())
                .totalItems(reliability.getTotalItems())
                .sampleSize(reliability.getSampleSize())
                .reliabilityStatus(reliability.getReliabilityStatus())
                .lastCalculatedAt(reliability.getLastCalculatedAt())
                .build();
    }

    private FlaggedItemSummary mapToFlaggedItemSummary(ItemStatistics stats) {
        AssessmentQuestion question = stats.getQuestion();
        BehavioralIndicator indicator = question != null ? question.getBehavioralIndicator() : null;
        Competency competency = indicator != null ? indicator.getCompetency() : null;

        return new FlaggedItemSummary(
                stats.getQuestionId(),
                question != null ? FlaggedItemSummary.truncateQuestionText(question.getQuestionText()) : null,
                competency != null ? competency.getName() : null,
                indicator != null ? indicator.getTitle() : null,
                stats.getDifficultyIndex(),
                stats.getDiscriminationIndex(),
                stats.getResponseCount(),
                stats.getValidityStatus(),
                stats.getDifficultyFlag(),
                stats.getDiscriminationFlag(),
                stats.getLastCalculatedAt()
        );
    }

    private List<String> generateRecommendations(ItemStatistics stats) {
        List<String> recommendations = new ArrayList<>();

        // Discrimination-based recommendations
        if (stats.getDiscriminationFlag() == DiscriminationFlag.NEGATIVE) {
            recommendations.add("CRITICAL: This item has negative discrimination. High performers fail while low performers succeed. Consider immediate retirement or complete revision.");
        } else if (stats.getDiscriminationFlag() == DiscriminationFlag.CRITICAL) {
            recommendations.add("This item shows poor discrimination (rpb < 0.1). Review question wording and answer options for clarity.");
        } else if (stats.getDiscriminationFlag() == DiscriminationFlag.WARNING) {
            recommendations.add("This item has marginal discrimination. Consider revising to better differentiate skill levels.");
        }

        // Difficulty-based recommendations
        if (stats.getDifficultyFlag() == DifficultyFlag.TOO_HARD) {
            recommendations.add("This item is too difficult (p < 0.2). Consider simplifying the question or providing clearer context.");
        } else if (stats.getDifficultyFlag() == DifficultyFlag.TOO_EASY) {
            recommendations.add("This item is too easy (p > 0.9). Consider increasing complexity or removing obvious answer options.");
        }

        // Distractor-based recommendations
        if (stats.getDistractorEfficiency() != null) {
            long nonFunctioning = stats.getDistractorEfficiency().values().stream()
                    .filter(rate -> rate != null && rate == 0.0)
                    .count();
            if (nonFunctioning > 0) {
                recommendations.add("This item has " + nonFunctioning + " non-functioning distractor(s) that are never selected. Consider revising these options to be more plausible.");
            }
        }

        // Data sufficiency
        if (stats.getResponseCount() != null && stats.getResponseCount() < 50) {
            recommendations.add("Insufficient data for reliable analysis. Need " + (50 - stats.getResponseCount()) + " more responses.");
        }

        // Declining discrimination
        if (stats.getPreviousDiscriminationIndex() != null && stats.getDiscriminationIndex() != null) {
            BigDecimal decline = stats.getPreviousDiscriminationIndex().subtract(stats.getDiscriminationIndex());
            if (decline.compareTo(new BigDecimal("0.05")) > 0) {
                recommendations.add("Discrimination has declined by " + String.format("%.2f", decline.doubleValue()) +
                        " since last calculation. Monitor for continued degradation.");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Item is performing within acceptable parameters. No immediate action required.");
        }

        return recommendations;
    }

    private String generateItemRecommendation(BigDecimal improvement) {
        double imp = improvement.doubleValue();
        if (imp >= 0.05) {
            return "Strongly consider removing this item - would significantly improve scale reliability";
        } else if (imp >= 0.02) {
            return "Consider revising or removing this item to improve reliability";
        } else {
            return "Minor impact - review if other issues are present";
        }
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
}
