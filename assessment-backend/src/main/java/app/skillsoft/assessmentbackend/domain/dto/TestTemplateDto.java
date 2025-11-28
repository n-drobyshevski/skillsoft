package app.skillsoft.assessmentbackend.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for TestTemplate entity response.
 */
public record TestTemplateDto(
        UUID id,
        String name,
        String description,
        List<UUID> competencyIds,
        Integer questionsPerIndicator,
        Integer timeLimitMinutes,
        Double passingScore,
        Boolean isActive,
        Boolean shuffleQuestions,
        Boolean shuffleOptions,
        Boolean allowSkip,
        Boolean allowBackNavigation,
        Boolean showResultsImmediately,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
