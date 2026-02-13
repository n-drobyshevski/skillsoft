package app.skillsoft.assessmentbackend.services.team.saga;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Context object for tracking saga execution state.
 * Used for compensation/rollback on failure.
 */
public class SagaContext {

    private UUID teamId;
    private UUID leaderId;
    private final List<UUID> addedMemberIds = new ArrayList<>();
    private final List<String> completedSteps = new ArrayList<>();
    private final List<String> errors = new ArrayList<>();

    public void recordStep(String step) {
        completedSteps.add(step);
    }

    public void recordError(String error) {
        errors.add(error);
    }

    // Getters and Setters
    public UUID getTeamId() {
        return teamId;
    }

    public void setTeamId(UUID teamId) {
        this.teamId = teamId;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(UUID leaderId) {
        this.leaderId = leaderId;
    }

    public List<UUID> getAddedMemberIds() {
        return addedMemberIds;
    }

    public void addMemberId(UUID memberId) {
        this.addedMemberIds.add(memberId);
    }

    public List<String> getCompletedSteps() {
        return completedSteps;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return "SagaContext{" +
                "teamId=" + teamId +
                ", leaderId=" + leaderId +
                ", addedMemberIds=" + addedMemberIds.size() +
                ", completedSteps=" + completedSteps +
                ", errors=" + errors +
                '}';
    }
}
