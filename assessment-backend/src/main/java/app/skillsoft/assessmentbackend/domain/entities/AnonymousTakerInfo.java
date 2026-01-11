package app.skillsoft.assessmentbackend.domain.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Information collected from anonymous test takers after completion.
 * Stored as JSONB in test_sessions.anonymous_taker_info column.
 *
 * <p>This class is used to capture basic identification information
 * from users who take tests via share links without authentication.
 * The information is collected after test completion to avoid
 * blocking the test-taking experience.</p>
 *
 * <p>Fields:</p>
 * <ul>
 *   <li>firstName - Required, taker's first name</li>
 *   <li>lastName - Required, taker's last name</li>
 *   <li>email - Optional, for follow-up communication</li>
 *   <li>notes - Optional, additional context (e.g., department, position)</li>
 *   <li>collectedAt - Auto-set timestamp when info was submitted</li>
 * </ul>
 *
 * @see TestSession#getAnonymousTakerInfo()
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnonymousTakerInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Taker's first name (required).
     */
    private String firstName;

    /**
     * Taker's last name (required).
     */
    private String lastName;

    /**
     * Taker's email address (optional).
     * Used for follow-up communication if provided.
     */
    private String email;

    /**
     * Additional notes or context (optional).
     * Examples: department, job title, referral source.
     * Maximum 500 characters.
     */
    private String notes;

    /**
     * Timestamp when this information was collected.
     * Auto-set when the taker submits their info.
     */
    private LocalDateTime collectedAt;

    // ========================================
    // Constructors
    // ========================================

    /**
     * Default constructor required for JSON deserialization.
     */
    public AnonymousTakerInfo() {
        // Required for Jackson
    }

    /**
     * Create taker info with required fields only.
     *
     * @param firstName Taker's first name
     * @param lastName  Taker's last name
     */
    public AnonymousTakerInfo(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.collectedAt = LocalDateTime.now();
    }

    /**
     * Create taker info with all fields.
     *
     * @param firstName Taker's first name
     * @param lastName  Taker's last name
     * @param email     Optional email address
     * @param notes     Optional notes/context
     */
    public AnonymousTakerInfo(String firstName, String lastName, String email, String notes) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.notes = notes;
        this.collectedAt = LocalDateTime.now();
    }

    // ========================================
    // Business Methods
    // ========================================

    /**
     * Get the taker's full display name.
     *
     * @return "firstName lastName" format
     */
    public String getDisplayName() {
        if (firstName == null && lastName == null) {
            return "Anonymous";
        }
        StringBuilder sb = new StringBuilder();
        if (firstName != null) {
            sb.append(firstName);
        }
        if (lastName != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(lastName);
        }
        return sb.toString();
    }

    /**
     * Check if email was provided.
     *
     * @return true if email is not null or blank
     */
    public boolean hasEmail() {
        return email != null && !email.isBlank();
    }

    // ========================================
    // Getters and Setters
    // ========================================

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }

    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }

    // ========================================
    // Object Overrides
    // ========================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnonymousTakerInfo that = (AnonymousTakerInfo) o;
        return Objects.equals(firstName, that.firstName) &&
               Objects.equals(lastName, that.lastName) &&
               Objects.equals(email, that.email) &&
               Objects.equals(collectedAt, that.collectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, email, collectedAt);
    }

    @Override
    public String toString() {
        return "AnonymousTakerInfo{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + (email != null ? email.substring(0, Math.min(3, email.length())) + "***" : null) + '\'' +
                ", collectedAt=" + collectedAt +
                '}';
    }
}
