package app.skillsoft.assessmentbackend.domain.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * User entity representing application users integrated with Clerk.js authentication.
 * 
 * This entity stores user information synchronized from Clerk,
 * linking to Clerk user records via the clerkId field.
 * 
 * Integration with Clerk.js:
 * - clerkId: Links to the external Clerk user record (User.id from Clerk)
 * - email: Synchronized with primary email from Clerk
 * - firstName/lastName: Synchronized with Clerk user data
 * - username: Clerk username
 * - imageUrl: Profile image URL from Clerk
 * - role: Application-specific role (from Clerk's publicMetadata)
 * 
 * @see <a href="https://clerk.com/docs/users/overview">Clerk User Management</a>
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_clerk_id", columnList = "clerk_id", unique = true),
    @Index(name = "idx_users_email", columnList = "email"),
    @Index(name = "idx_users_role", columnList = "role"),
    @Index(name = "idx_users_username", columnList = "username")
})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Clerk.js user ID - links to external Clerk user record.
     * This should match the 'id' field from Clerk's User object.
     */
    @Column(name = "clerk_id", nullable = false, unique = true, length = 100)
    @NotBlank(message = "Clerk ID is required")
    @Size(max = 100, message = "Clerk ID must not exceed 100 characters")
    private String clerkId;

    /**
     * User's email address - synchronized with Clerk's primary email.
     */
    @Column(name = "email", nullable = true, length = 255)
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    /**
     * User's username from Clerk.
     */
    @Column(name = "username", length = 100)
    @Size(max = 100, message = "Username must not exceed 100 characters")
    private String username;

    /**
     * User's first name - synchronized from Clerk.
     */
    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    /**
     * User's last name - synchronized from Clerk.
     */
    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    /**
     * User's profile image URL from Clerk.
     */
    @Column(name = "image_url", length = 500)
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    /**
     * Whether the user has a custom profile image in Clerk.
     */
    @Column(name = "has_image", nullable = false)
    private boolean hasImage = false;

    /**
     * Application-specific role for authorization.
     * Stored as metadata in Clerk's publicMetadata for session token access.
     */
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull(message = "User role is required")
    private UserRole role;

    /**
     * Whether the user account is active and can access the system.
     */
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    /**
     * Whether the user is banned in Clerk.
     */
    @Column(name = "banned", nullable = false)
    private boolean banned = false;

    /**
     * Whether the user is locked in Clerk.
     */
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    /**
     * Additional user preferences or settings stored as JSON.
     * Can include UI preferences, assessment settings, etc.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferences", columnDefinition = "jsonb default '{}'::jsonb")
    private String preferences = "{}";

    /**
     * When this user record was created in our system.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this user record was last updated.
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * When the user was created in Clerk (from Clerk's created_at).
     */
    @Column(name = "clerk_created_at")
    private LocalDateTime clerkCreatedAt;

    /**
     * When the user last logged in (updated via webhook or manual sync).
     */
    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    /**
     * When the user last signed in to Clerk (from Clerk's last_sign_in_at).
     */
    @Column(name = "last_sign_in_at")
    private LocalDateTime lastSignInAt;

    // Constructors
    public User() {
        // Default constructor required by JPA
    }

    public User(String clerkId, String email, UserRole role) {
        this.clerkId = clerkId;
        this.email = email;
        this.role = role;
        this.isActive = true;
    }

    public User(String clerkId, String email, String firstName, String lastName, UserRole role) {
        this(clerkId, email, role);
        this.firstName = firstName;
        this.lastName = lastName;
    }

    /**
     * Full constructor with all Clerk fields.
     */
    public User(String clerkId, String email, String username, String firstName, String lastName, 
                String imageUrl, boolean hasImage, boolean banned, boolean locked,
                LocalDateTime clerkCreatedAt, LocalDateTime lastSignInAt, UserRole role) {
        this.clerkId = clerkId;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.imageUrl = imageUrl;
        this.hasImage = hasImage;
        this.banned = banned;
        this.locked = locked;
        this.clerkCreatedAt = clerkCreatedAt;
        this.lastSignInAt = lastSignInAt;
        this.role = role;
        this.isActive = !banned && !locked;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClerkId() {
        return clerkId;
    }

    public void setClerkId(String clerkId) {
        this.clerkId = clerkId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getClerkCreatedAt() {
        return clerkCreatedAt;
    }

    public void setClerkCreatedAt(LocalDateTime clerkCreatedAt) {
        this.clerkCreatedAt = clerkCreatedAt;
    }

    public LocalDateTime getLastSignInAt() {
        return lastSignInAt;
    }

    public void setLastSignInAt(LocalDateTime lastSignInAt) {
        this.lastSignInAt = lastSignInAt;
    }

    // Business methods
    
    /**
     * Returns the user's full name by combining first and last names.
     * @return Full name, username, email if available, or clerk ID as fallback
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else if (username != null) {
            return username;
        } else if (email != null) {
            return email;
        }
        return clerkId; // Fallback to clerk ID
    }

    /**
     * Checks if the user has administrative privileges.
     * @return true if user is an admin
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Checks if the user can edit content (editor or admin).
     * @return true if user can edit content
     */
    public boolean canEditContent() {
        return role == UserRole.EDITOR || role == UserRole.ADMIN;
    }

    /**
     * Checks if the user can access the system (active, not banned, not locked).
     * @return true if user can access the system
     */
    public boolean canAccess() {
        return isActive && !banned && !locked;
    }

    /**
     * Updates the last login timestamp to current time.
     */
    public void updateLastLogin() {
        this.lastLogin = LocalDateTime.now();
    }

    // Object overrides
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id) && Objects.equals(clerkId, user.clerkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clerkId);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", clerkId='" + clerkId + '\'' +
                ", email='" + email + '\'' +
                ", username='" + username + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", banned=" + banned +
                ", locked=" + locked +
                ", createdAt=" + createdAt +
                ", lastSignInAt=" + lastSignInAt +
                '}';
    }
}