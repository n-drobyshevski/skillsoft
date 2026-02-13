package app.skillsoft.assessmentbackend.repository;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousSessionRateLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing anonymous session rate limiting.
 *
 * <p>Provides methods to track and enforce rate limits on anonymous
 * test session creation by IP address.</p>
 *
 * @see AnonymousSessionRateLimit
 */
@Repository
public interface AnonymousSessionRateLimitRepository extends JpaRepository<AnonymousSessionRateLimit, UUID> {

    /**
     * Find rate limit entry by IP address.
     *
     * @param ipAddress The client IP address
     * @return Optional containing the rate limit entry if found
     */
    Optional<AnonymousSessionRateLimit> findByIpAddress(String ipAddress);

    /**
     * Check if an IP address exists in the rate limit table.
     *
     * @param ipAddress The client IP address
     * @return true if entry exists
     */
    boolean existsByIpAddress(String ipAddress);

    /**
     * Delete expired rate limit entries.
     * Entries are considered expired if their windowStart is before the cutoff time.
     * This should be run periodically to clean up the table.
     *
     * @param cutoff Delete entries with windowStart before this time
     * @return Number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM AnonymousSessionRateLimit r WHERE r.windowStart < :cutoff AND r.blockedUntil IS NULL")
    int deleteExpiredEntries(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Delete all entries that are no longer blocked and have expired windows.
     * More aggressive cleanup than deleteExpiredEntries.
     *
     * @param windowCutoff Delete entries with windowStart before this time
     * @param blockCutoff Delete blocked entries with blockedUntil before this time
     * @return Number of deleted entries
     */
    @Modifying
    @Query("DELETE FROM AnonymousSessionRateLimit r WHERE " +
           "(r.windowStart < :windowCutoff AND r.blockedUntil IS NULL) OR " +
           "(r.blockedUntil IS NOT NULL AND r.blockedUntil < :blockCutoff)")
    int deleteStaleEntries(
            @Param("windowCutoff") LocalDateTime windowCutoff,
            @Param("blockCutoff") LocalDateTime blockCutoff);

    /**
     * Count currently blocked IPs.
     *
     * @param now Current timestamp
     * @return Number of IPs currently blocked
     */
    @Query("SELECT COUNT(r) FROM AnonymousSessionRateLimit r WHERE r.blockedUntil > :now")
    long countBlockedIps(@Param("now") LocalDateTime now);

    /**
     * Find all currently blocked entries.
     *
     * @param now Current timestamp
     * @return List of blocked rate limit entries
     */
    @Query("SELECT r FROM AnonymousSessionRateLimit r WHERE r.blockedUntil > :now")
    java.util.List<AnonymousSessionRateLimit> findBlockedEntries(@Param("now") LocalDateTime now);
}
