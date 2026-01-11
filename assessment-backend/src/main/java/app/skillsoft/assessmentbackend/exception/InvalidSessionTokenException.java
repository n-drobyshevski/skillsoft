package app.skillsoft.assessmentbackend.exception;

/**
 * Exception thrown when an anonymous session token is invalid.
 *
 * This exception results in HTTP 401 UNAUTHORIZED responses.
 * It indicates that the X-Session-Token header is missing, malformed,
 * or does not match any active anonymous session.
 *
 * @author SkillSoft Development Team
 */
public class InvalidSessionTokenException extends RuntimeException {

    private final String messageRu;

    /**
     * Constructs a new invalid session token exception.
     *
     * @param message   English error message
     * @param messageRu Russian error message
     */
    public InvalidSessionTokenException(String message, String messageRu) {
        super(message);
        this.messageRu = messageRu;
    }

    /**
     * Constructs a new invalid session token exception with default messages.
     */
    public InvalidSessionTokenException() {
        this(
                "Invalid or missing session token",
                "Неверный или отсутствующий токен сессии"
        );
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
