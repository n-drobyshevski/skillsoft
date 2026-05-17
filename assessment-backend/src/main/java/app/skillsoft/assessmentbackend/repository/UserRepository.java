package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.User;
import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity operations.
 * Provides methods for user management and Clerk.js integration.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by Clerk ID.
     * Primary method for linking Clerk authentication to our User entity.
     */
    Optional<User> findByClerkId(String clerkId);

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user exists with the given Clerk ID.
     */
    boolean existsByClerkId(String clerkId);

    /**
     * Check if a user exists with the given email.
     */
    boolean existsByEmail(String email);

    /**
     * Find all active users.
     */
    List<User> findByIsActiveTrue();

    /**
     * Find users by role.
     */
    List<User> findByRole(UserRole role);

    /**
     * Find active users by role.
     */
    List<User> findByRoleAndIsActiveTrue(UserRole role);

    /**
     * Find users created after a specific date.
     */
    List<User> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Count active users created after a specific date.
     * Used for "new users" navigation badge (rolling 7-day window).
     */
    long countByCreatedAtAfterAndIsActiveTrue(LocalDateTime date);

    /**
     * Find users who haven't logged in since a specific date.
     */
    List<User> findByLastLoginBeforeOrLastLoginIsNull(LocalDateTime date);

    /**
     * Update user's last login timestamp.
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLogin = :loginTime WHERE u.clerkId = :clerkId")
    int updateLastLoginByClerkId(@Param("clerkId") String clerkId, @Param("loginTime") LocalDateTime loginTime);

    /**
     * Deactivate user by Clerk ID (soft delete).
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.clerkId = :clerkId")
    int deactivateByClerkId(@Param("clerkId") String clerkId);

    /**
     * Count users by role.
     */
    @Query("SELECT u.role, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.role")
    List<Object[]> countActiveUsersByRole();

    /**
     * Find users with email containing the search term (case-insensitive).
     */
    List<User> findByEmailContainingIgnoreCase(String emailFragment);

    /**
     * Find users by first or last name containing the search term (case-insensitive).
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR " +
           "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContaining(@Param("name") String name);

    /**
     * Find multiple users by their Clerk IDs.
     * Used for batch user enrichment in activity feeds (avoiding N+1 queries).
     */
    @Query("SELECT u FROM User u WHERE u.clerkId IN :clerkIds")
    List<User> findByClerkIdIn(@Param("clerkIds") Iterable<String> clerkIds);
}