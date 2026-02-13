package app.skillsoft.assessmentbackend.exception;

/**
 * Exception thrown when a share link is invalid, expired, revoked, or has reached its usage limit.
 *
 * This exception results in HTTP 400 BAD REQUEST responses with specific error codes
 * that can be used by the frontend to display appropriate messages.
 *
 * Error codes:
 * - LINK_NOT_FOUND: The share link token does not exist
 * - LINK_EXPIRED: The share link has passed its expiration date
 * - LINK_REVOKED: The share link was revoked by the owner
 * - LINK_MAX_USES_REACHED: The share link has reached its maximum usage count
 * - TEMPLATE_NOT_LINK_VISIBLE: The template visibility is not set to LINK
 *
 * @author SkillSoft Development Team
 */
public class ShareLinkException extends RuntimeException {

    /**
     * Error codes for different share link failure scenarios.
     */
    public enum ErrorCode {
        LINK_NOT_FOUND,
        LINK_EXPIRED,
        LINK_REVOKED,
        LINK_MAX_USES_REACHED,
        TEMPLATE_NOT_LINK_VISIBLE,
        TEMPLATE_NOT_READY
    }

    private final ErrorCode errorCode;
    private final String messageRu;

    /**
     * Constructs a new share link exception.
     *
     * @param errorCode The specific error code
     * @param message   English error message
     * @param messageRu Russian error message
     */
    public ShareLinkException(ErrorCode errorCode, String message, String messageRu) {
        super(message);
        this.errorCode = errorCode;
        this.messageRu = messageRu;
    }

    /**
     * Constructs a new share link exception with just an error code.
     * Uses default messages based on the error code.
     *
     * @param errorCode The specific error code
     */
    public ShareLinkException(ErrorCode errorCode) {
        this(errorCode, getDefaultMessage(errorCode), getDefaultMessageRu(errorCode));
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessageRu() {
        return messageRu;
    }

    private static String getDefaultMessage(ErrorCode code) {
        return switch (code) {
            case LINK_NOT_FOUND -> "Share link not found";
            case LINK_EXPIRED -> "This share link has expired";
            case LINK_REVOKED -> "This share link has been revoked";
            case LINK_MAX_USES_REACHED -> "This share link has reached its maximum usage limit";
            case TEMPLATE_NOT_LINK_VISIBLE -> "This template is not available via share link";
            case TEMPLATE_NOT_READY -> "This template is not ready for testing";
        };
    }

    private static String getDefaultMessageRu(ErrorCode code) {
        return switch (code) {
            case LINK_NOT_FOUND -> "Ссылка не найдена";
            case LINK_EXPIRED -> "Срок действия ссылки истек";
            case LINK_REVOKED -> "Ссылка была отозвана";
            case LINK_MAX_USES_REACHED -> "Достигнут лимит использования ссылки";
            case TEMPLATE_NOT_LINK_VISIBLE -> "Шаблон недоступен по ссылке";
            case TEMPLATE_NOT_READY -> "Шаблон не готов к тестированию";
        };
    }
}
