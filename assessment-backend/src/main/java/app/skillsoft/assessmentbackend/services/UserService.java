package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for User operations.
 * Handles user management and Clerk.js integration.
 */
public interface UserService {

    /**
     * Create a new user from Clerk webhook data.
     * @param clerkId Clerk user ID
     * @param email User's email
     * @param firstName User's first name (optional)
     * @param lastName User's last name (optional)
     * @param role User's role (defaults to USER if not specified)
     * @return Created user
     */
    User createUser(String clerkId, String email, String firstName, String lastName, UserRole role);

    /**
     * Find user by Clerk ID.
     * @param clerkId Clerk user ID
     * @return Optional User
     */
    Optional<User> findByClerkId(String clerkId);

    /**
     * Find user by email.
     * @param email User's email
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by internal UUID.
     * @param id User's UUID
     * @return Optional User
     */
    Optional<User> findById(UUID id);

    /**
     * Get all users.
     * @return List of all users
     */
    List<User> findAllUsers();

    /**
     * Get all active users.
     * @return List of active users
     */
    List<User> findActiveUsers();

    /**
     * Get users by role.
     * @param role User role to filter by
     * @return List of users with specified role
     */
    List<User> findUsersByRole(UserRole role);

    /**
     * Update user information.
     * @param user User to update
     * @return Updated user
     */
    User updateUser(User user);

    /**
     * Update user role.
     * @param clerkId Clerk user ID
     * @param newRole New role to assign
     * @return Updated user
     */
    User updateUserRole(String clerkId, UserRole newRole);

    /**
     * Update user's last login timestamp.
     * @param clerkId Clerk user ID
     * @param loginTime Login timestamp
     * @return Whether update was successful
     */
    boolean updateLastLogin(String clerkId, LocalDateTime loginTime);

    /**
     * Deactivate user (soft delete).
     * @param clerkId Clerk user ID
     * @return Whether deactivation was successful
     */
    boolean deactivateUser(String clerkId);

    /**
     * Reactivate user.
     * @param clerkId Clerk user ID
     * @return Whether reactivation was successful
     */
    boolean reactivateUser(String clerkId);

    /**
     * Delete user permanently.
     * @param clerkId Clerk user ID
     * @return Whether deletion was successful
     */
    boolean deleteUser(String clerkId);

    /**
     * Sync user data from Clerk (useful for webhooks).
     * @param clerkId Clerk user ID
     * @param email Updated email
     * @param firstName Updated first name
     * @param lastName Updated last name
     * @return Updated user
     */
    User syncUserFromClerk(String clerkId, String email, String firstName, String lastName);

    /**
     * Check if user exists by Clerk ID.
     * @param clerkId Clerk user ID
     * @return true if user exists
     */
    boolean existsByClerkId(String clerkId);

    /**
     * Search users by name or email.
     * @param query Search query
     * @return List of matching users
     */
    List<User> searchUsers(String query);

    /**
     * Get user statistics by role.
     * @return Map of role counts
     */
    List<Object[]> getUserStatsByRole();

    /**
     * Bulk sync users from Clerk.
     * Creates new users or updates existing ones based on Clerk ID.
     * @param clerkUsers List of user data from Clerk
     * @return SyncResult with counts of created, updated, and failed users
     */
    SyncResult bulkSyncFromClerk(List<ClerkUserData> clerkUsers);

    /**
     * Data transfer object for Clerk user data during bulk sync.
     */
    record ClerkUserData(
        String clerkId,
        String email,
        String username,
        String firstName,
        String lastName,
        String imageUrl,
        boolean hasImage,
        boolean banned,
        boolean locked,
        Long clerkCreatedAt,  // Unix timestamp in milliseconds
        Long lastSignInAt,    // Unix timestamp in milliseconds
        String role
    ) {}

    /**
     * Result of bulk sync operation.
     */
    record SyncResult(
        int created,
        int updated,
        int failed,
        int total,
        List<String> errors
    ) {}
}