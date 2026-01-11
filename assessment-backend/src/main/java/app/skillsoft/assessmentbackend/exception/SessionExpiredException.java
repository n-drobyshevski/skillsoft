package app.skillsoft.assessmentbackend.exception;

import java.util.UUID;

/**
 * Exception thrown when an anonymous session has expired.
 *
 * This exception results in HTTP 400 BAD REQUEST responses.
 * Anonymous sessions expire 24 hours after creation if not completed.
 *
 * @author SkillSoft Development Team
 */
public class SessionExpiredException extends RuntimeException {

    private final UUID sessionId;
    private final String messageRu;

    /**
     * Constructs a new session expired exception.
     *
     * @param sessionId The expired session ID
     * @param message   English error message
     * @param messageRu Russian error message
     */
    public SessionExpiredException(UUID sessionId, String message, String messageRu) {
        super(message);
        this.sessionId = sessionId;
        this.messageRu = messageRu;
    }

    /**
     * Constructs a new session expired exception with default messages.
     *
     * @param sessionId The expired session ID
     */
    public SessionExpiredException(UUID sessionId) {
        this(
                sessionId,
                "This session has expired. Please start a new test.",
                "Сессия истекла. Пожалуйста, начните новый тест."
        );
    }

    /**
     * Gets the expired session ID.
     *
     * @return Session UUID
     */
    public UUID getSessionId() {
        return sessionId;
    }

    /**
     * Gets the Russian error message.
     *
     * @return Russian message
     */
    public String getMessageRu() {
        return messageRu;
    }
}
