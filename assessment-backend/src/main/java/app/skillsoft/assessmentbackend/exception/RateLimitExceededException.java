package app.skillsoft.assessmentbackend.exception;

/**
 * Exception thrown when a rate limit has been exceeded.
 *
 * This exception results in HTTP 429 TOO MANY REQUESTS responses.
 * The retryAfterSeconds field indicates when the client can retry.
 *
 * @author SkillSoft Development Team
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;
    private final String messageRu;

    /**
     * Constructs a new rate limit exceeded exception.
     *
     * @param message           English error message
     * @param messageRu         Russian error message
     * @param retryAfterSeconds Seconds until the client can retry
     */
    public RateLimitExceededException(String message, String messageRu, long retryAfterSeconds) {
        super(message);
        this.messageRu = messageRu;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Constructs a new rate limit exceeded exception with default messages.
     *
     * @param retryAfterSeconds Seconds until the client can retry
     */
    public RateLimitExceededException(long retryAfterSeconds) {
        this(
                "Too many requests. Please try again later.",
                "Слишком много запросов. Пожалуйста, попробуйте позже.",
                retryAfterSeconds
        );
    }

    /**
     * Gets the number of seconds until the client can retry.
     *
     * @return Seconds to wait before retrying
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
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
