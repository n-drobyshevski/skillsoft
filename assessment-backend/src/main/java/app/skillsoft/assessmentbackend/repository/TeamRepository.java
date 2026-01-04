package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.Team;
import app.skillsoft.assessmentbackend.domain.entities.TeamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Team entity operations.
 * Provides methods for team management and queries.
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    /**
     * Find teams by status.
     */
    List<Team> findByStatus(TeamStatus status);

    /**
     * Find teams by status with pagination.
     */
    Page<Team> findByStatus(TeamStatus status, Pageable pageable);

    /**
     * Check if a team exists with given ID and status.
     * Used for validating team eligibility for TEAM_FIT assessments.
     */
    boolean existsByIdAndStatus(UUID id, TeamStatus status);

    /**
     * Find all active teams (status = ACTIVE).
     */
    @Query("SELECT t FROM Team t WHERE t.status = 'ACTIVE'")
    List<Team> findAllActive();

    /**
     * Find teams created by a specific user.
     */
    List<Team> findByCreatedById(UUID createdById);

    /**
     * Find teams led by a specific user.
     */
    List<Team> findByLeaderId(UUID leaderId);

    /**
     * Search teams by name (case-insensitive).
     */
    @Query("SELECT t FROM Team t WHERE LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Team> searchByName(@Param("search") String search, Pageable pageable);

    /**
     * Search teams by name and filter by status.
     */
    @Query("SELECT t FROM Team t WHERE " +
           "LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')) AND " +
           "(:status IS NULL OR t.status = :status)")
    Page<Team> searchByNameAndStatus(
            @Param("search") String search,
            @Param("status") TeamStatus status,
            Pageable pageable);

    /**
     * Find team with members eagerly fetched.
     * Avoids N+1 queries when accessing members.
     */
    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.members m LEFT JOIN FETCH m.user WHERE t.id = :teamId")
    Optional<Team> findByIdWithMembers(@Param("teamId") UUID teamId);

    /**
     * Find team with leader eagerly fetched.
     */
    @Query("SELECT t FROM Team t LEFT JOIN FETCH t.leader WHERE t.id = :teamId")
    Optional<Team> findByIdWithLeader(@Param("teamId") UUID teamId);

    /**
     * Count teams by status.
     */
    long countByStatus(TeamStatus status);

    /**
     * Count teams with active members greater than threshold.
     */
    @Query("SELECT COUNT(DISTINCT t) FROM Team t JOIN t.members m " +
           "WHERE t.status = :status AND m.isActive = true " +
           "GROUP BY t HAVING COUNT(m) >= :minMembers")
    long countByStatusAndMinMembers(
            @Param("status") TeamStatus status,
            @Param("minMembers") long minMembers);

    /**
     * Find teams that have a specific user as a member (active membership).
     */
    @Query("SELECT t FROM Team t JOIN t.members m " +
           "WHERE m.user.id = :userId AND m.isActive = true")
    List<Team> findByMemberUserId(@Param("userId") UUID userId);

    /**
     * Find teams by member's Clerk ID.
     */
    @Query("SELECT t FROM Team t JOIN t.members m " +
           "WHERE m.user.clerkId = :clerkId AND m.isActive = true")
    List<Team> findByMemberClerkId(@Param("clerkId") String clerkId);
}
