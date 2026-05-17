package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.dto.comparison.CandidateComparisonDto;

import java.util.List;
import java.util.UUID;

/**
 * Service for comparing multiple candidate test results within a Team Fit or Job Fit assessment.
 * Provides ranked summaries, competency comparisons, gap coverage analysis,
 * and pairwise complementarity scoring.
 */
public interface CandidateComparisonService {

    /**
     * Compare multiple test results for the same TEAM_FIT or JOB_FIT template.
     *
     * @param resultIds  List of result UUIDs to compare (2-5 results required)
     * @param templateId The template UUID all results must belong to
     * @return Comprehensive comparison DTO with rankings, gap analysis, and complementarity
     * @throws IllegalArgumentException if validation fails (wrong count, mismatched templates, unsupported goal)
     */
    CandidateComparisonDto compareResults(List<UUID> resultIds, UUID templateId);
}
