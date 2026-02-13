package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.TeamMember;
import app.skillsoft.assessmentbackend.domain.entities.TeamMemberRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TeamMember entity operations.
 * Manages the many-to-many relationship between Teams and Users.
 */
@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    /**
     * Find all active members of a team.
     */
    List<TeamMember> findByTeamIdAndIsActiveTrue(UUID teamId);

    /**
     * Find all members of a team (including inactive).
     */
    List<TeamMember> findByTeamId(UUID teamId);

    /**
     * Find specific team member by team and user.
     */
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Check if a user is an active member of a team.
     */
    boolean existsByTeamIdAndUserIdAndIsActiveTrue(UUID teamId, UUID userId);

    /**
     * Check if a user is a member of a team (active or inactive).
     */
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    /**
     * Find all team memberships for a user.
     */
    List<TeamMember> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find team memberships by user's Clerk ID.
     */
    @Query("SELECT tm FROM TeamMember tm JOIN tm.user u WHERE u.clerkId = :clerkId AND tm.isActive = true")
    List<TeamMember> findByUserClerkIdAndIsActiveTrue(@Param("clerkId") String clerkId);

    /**
     * Find team IDs where user is an active member.
     * Useful for cache invalidation when a user's test result is completed.
     */
    @Query("SELECT tm.team.id FROM TeamMember tm JOIN tm.user u WHERE u.clerkId = :clerkId AND tm.isActive = true")
    List<UUID> findTeamIdsByUserClerkId(@Param("clerkId") String clerkId);

    /**
     * Find the leader of a team.
     */
    Optional<TeamMember> findByTeamIdAndRole(UUID teamId, TeamMemberRole role);

    /**
     * Count active members in a team.
     */
    long countByTeamIdAndIsActiveTrue(UUID teamId);

    /**
     * Find members by role in a team.
     */
    List<TeamMember> findByTeamIdAndRoleAndIsActiveTrue(UUID teamId, TeamMemberRole role);

    /**
     * Find member with user eagerly fetched.
     */
    @Query("SELECT tm FROM TeamMember tm JOIN FETCH tm.user WHERE tm.team.id = :teamId AND tm.isActive = true")
    List<TeamMember> findByTeamIdWithUser(@Param("teamId") UUID teamId);

    /**
     * Delete all members for a team.
     * Used when deleting a team permanently.
     */
    void deleteByTeamId(UUID teamId);

    /**
     * Count teams a user is a member of.
     */
    @Query("SELECT COUNT(DISTINCT tm.team.id) FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true")
    long countTeamsByUserId(@Param("userId") UUID userId);

    /**
     * Find team IDs where user is an active member (by user UUID).
     * Used for template visibility access checks.
     */
    @Query("SELECT tm.team.id FROM TeamMember tm WHERE tm.user.id = :userId AND tm.isActive = true")
    List<UUID> findActiveTeamIdsByUserId(@Param("userId") UUID userId);
}
