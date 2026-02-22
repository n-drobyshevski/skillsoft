package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.CompetencyPassportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CompetencyPassportEntity persistence operations.
 */
@Repository
public interface CompetencyPassportRepository extends JpaRepository<CompetencyPassportEntity, UUID> {

    /**
     * Find a passport by Clerk user ID (any expiration state).
     */
    Optional<CompetencyPassportEntity> findByClerkUserId(String clerkUserId);

    /**
     * Find a valid (non-expired) passport by Clerk user ID.
     *
     * @param clerkUserId the Clerk user ID
     * @param now         current timestamp for expiration comparison
     * @return the passport if it exists and has not expired
     */
    @Query("SELECT p FROM CompetencyPassportEntity p " +
           "WHERE p.clerkUserId = :clerkUserId AND p.expiresAt > :now")
    Optional<CompetencyPassportEntity> findValidByClerkUserId(
            @Param("clerkUserId") String clerkUserId,
            @Param("now") LocalDateTime now);

    /**
     * Check if a valid (non-expired) passport exists for the given Clerk user ID.
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
           "FROM CompetencyPassportEntity p " +
           "WHERE p.clerkUserId = :clerkUserId AND p.expiresAt > :now")
    boolean existsValidByClerkUserId(
            @Param("clerkUserId") String clerkUserId,
            @Param("now") LocalDateTime now);

    /**
     * Check if any passport exists for the given Clerk user ID (regardless of expiration).
     */
    boolean existsByClerkUserId(String clerkUserId);
}
