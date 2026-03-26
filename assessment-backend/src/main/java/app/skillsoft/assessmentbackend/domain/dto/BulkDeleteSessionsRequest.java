package app.skillsoft.assessmentbackend.domain.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record BulkDeleteSessionsRequest(
        @NotEmpty(message = "Session IDs list cannot be empty")
        @Size(max = 50, message = "Cannot delete more than 50 sessions at once")
        List<@NotNull(message = "Session ID cannot be null") UUID> sessionIds
) {}
