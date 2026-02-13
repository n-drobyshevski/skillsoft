package app.skillsoft.assessmentbackend.domain.dto.team;

import app.skillsoft.assessmentbackend.domain.entities.Team;

import java.util.List;

/**
 * Result DTO for team creation saga.
 */
public record TeamCreationResult(
        boolean success,
        Team team,
        List<String> warnings,
        String errorMessage,
        List<String> errors
) {
    public static TeamCreationResult success(Team team, List<String> warnings) {
        return new TeamCreationResult(true, team, warnings, null, List.of());
    }

    public static TeamCreationResult failure(String errorMessage, List<String> errors) {
        return new TeamCreationResult(false, null, List.of(), errorMessage, errors);
    }

    public boolean isSuccess() {
        return success;
    }

    public Team getTeam() {
        return team;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public List<String> getErrors() {
        return errors;
    }
}
