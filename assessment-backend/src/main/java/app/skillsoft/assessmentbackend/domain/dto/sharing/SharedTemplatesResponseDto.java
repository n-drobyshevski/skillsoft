package app.skillsoft.assessmentbackend.domain.dto.sharing;

import java.util.List;

/**
 * Response DTO for listing templates shared with the current user.
 * Matches frontend SharedTemplatesResponse interface.
 */
public record SharedTemplatesResponseDto(
        List<SharedTemplateItemDto> items,
        int total
) {
    /**
     * Create response from list of items.
     */
    public static SharedTemplatesResponseDto of(List<SharedTemplateItemDto> items) {
        return new SharedTemplatesResponseDto(items, items.size());
    }

    /**
     * Create empty response.
     */
    public static SharedTemplatesResponseDto empty() {
        return new SharedTemplatesResponseDto(List.of(), 0);
    }
}
