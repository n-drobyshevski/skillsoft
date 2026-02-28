package app.skillsoft.assessmentbackend.domain.dto;

import java.util.UUID;

/**
 * DTO representing a competency resolved from an O*NET benchmark profile.
 * Maps an O*NET benchmark name to an internal competency with its benchmark score.
 *
 * Used by the library panel to restrict available competencies in JOB_FIT mode,
 * and by the assembler to ensure canvas and assembly agree on which competencies are used.
 */
public record ResolvedOnetCompetencyDto(
    UUID id,
    String name,
    String category,
    String onetBenchmarkName,
    double benchmarkScore
) {}
