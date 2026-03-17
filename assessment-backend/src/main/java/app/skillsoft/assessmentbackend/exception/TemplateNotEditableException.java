package app.skillsoft.assessmentbackend.exception;

import app.skillsoft.assessmentbackend.domain.entities.TemplateStatus;

/**
 * Thrown when an operation is attempted on a template that is not in DRAFT status.
 * Only DRAFT templates can be modified.
 */
public class TemplateNotEditableException extends RuntimeException {

    private final TemplateStatus currentStatus;

    public TemplateNotEditableException(TemplateStatus currentStatus) {
        super(String.format(
                "Cannot modify template in %s status. Only DRAFT templates can be edited.",
                currentStatus));
        this.currentStatus = currentStatus;
    }

    public TemplateStatus getCurrentStatus() {
        return currentStatus;
    }
}
