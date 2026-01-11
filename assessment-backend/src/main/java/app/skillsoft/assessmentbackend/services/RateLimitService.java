package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousSessionRateLimit;
import app.skillsoft.assessmentbackend.exception.RateLimitExceededException;
import app.skillsoft.assessmentbackend.repository.AnonymousSessionRateLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing IP-based rate limiting for anonymous sessions.
 *
 * <p>Rate limiting strategy:</p>
 * <ul>
 *   <li>Window: 1 hour rolling window</li>
 *   <li>Limit: 10 sessions per IP per window</li>
 *   <li>Block Duration: 1 hour after exceeding limit</li>
 * </ul>
 *
 * <p>The service uses the AnonymousSessionRateLimit entity to track
 * session creation attempts per IP address. Expired entries are
 * cleaned up periodically.</p>
 *
 * @author SkillSoft Development Team
 */
@Service
@Transactional
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Hours before rate limit entries are cleaned up.
     */
    private static final int CLEANUP_HOURS = 24;

    private final AnonymousSessionRateLimitRepository rateLimitRepository;

    public RateLimitService(AnonymousSessionRateLimitRepository rateLimitRepository) {
        this.rateLimitRepository = rateLimitRepository;
    }

    /**
     * Check if a session can be created from the given IP address.
     *
     * <p>If the IP is not blocked and under the limit, increments the count
     * and returns true. Otherwise, throws RateLimitExceededException.</p>
     *
     * @param ipAddress The client IP address
     * @throws RateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            log.warn("Rate limit check called with null or blank IP address");
            return; // Allow if IP is unknown
        }

        Optional<AnonymousSessionRateLimit> existingLimit = rateLimitRepository.findByIpAddress(ipAddress);

        if (existingLimit.isPresent()) {
            AnonymousSessionRateLimit limit = existingLimit.get();

            // Check if currently blocked
            if (limit.isBlocked()) {
                long retryAfter = limit.getSecondsUntilUnblocked();
                log.info("Rate limit: IP {} is blocked for {} more seconds", ipAddress, retryAfter);
                throw new RateLimitExceededException(retryAfter);
            }

            // Try to increment
            if (!limit.incrementAndCheck()) {
                long retryAfter = limit.getSecondsUntilUnblocked();
                log.info("Rate limit exceeded for IP {}: {} sessions in current window",
                        ipAddress, limit.getSessionCount());
                rateLimitRepository.save(limit);
                throw new RateLimitExceededException(retryAfter);
            }

            rateLimitRepository.save(limit);
            log.debug("Rate limit: IP {} at {}/{} sessions",
                    ipAddress, limit.getSessionCount(), AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
        } else {
            // First session from this IP
            AnonymousSessionRateLimit newLimit = new AnonymousSessionRateLimit(ipAddress);
            rateLimitRepository.save(newLimit);
            log.debug("Rate limit: New entry for IP {}", ipAddress);
        }
    }

    /**
     * Get the number of remaining sessions allowed for an IP.
     *
     * @param ipAddress The client IP address
     * @return Number of remaining sessions, or max if not tracked
     */
    @Transactional(readOnly = true)
    public int getRemainingAllowed(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW;
        }

        return rateLimitRepository.findByIpAddress(ipAddress)
                .map(AnonymousSessionRateLimit::getRemainingAllowed)
                .orElse(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
    }

    /**
     * Check if an IP is currently blocked.
     *
     * @param ipAddress The client IP address
     * @return true if blocked
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return false;
        }

        return rateLimitRepository.findByIpAddress(ipAddress)
                .map(AnonymousSessionRateLimit::isBlocked)
                .orElse(false);
    }

    /**
     * Get seconds until an IP is unblocked.
     *
     * @param ipAddress The client IP address
     * @return Seconds until unblocked, or 0 if not blocked
     */
    @Transactional(readOnly = true)
    public long getSecondsUntilUnblocked(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return 0;
        }

        return rateLimitRepository.findByIpAddress(ipAddress)
                .map(AnonymousSessionRateLimit::getSecondsUntilUnblocked)
                .orElse(0L);
    }

    /**
     * Manually unblock an IP address.
     * Used for admin operations.
     *
     * @param ipAddress The IP address to unblock
     * @return true if an entry was found and unblocked
     */
    public boolean unblock(String ipAddress) {
        Optional<AnonymousSessionRateLimit> limit = rateLimitRepository.findByIpAddress(ipAddress);
        if (limit.isPresent()) {
            limit.get().resetWindow();
            rateLimitRepository.save(limit.get());
            log.info("Rate limit: Manually unblocked IP {}", ipAddress);
            return true;
        }
        return false;
    }

    /**
     * Scheduled cleanup of expired rate limit entries.
     * Runs every hour to remove stale entries.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredEntries() {
        LocalDateTime windowCutoff = LocalDateTime.now().minusHours(CLEANUP_HOURS);
        LocalDateTime blockCutoff = LocalDateTime.now();

        int deleted = rateLimitRepository.deleteStaleEntries(windowCutoff, blockCutoff);
        if (deleted > 0) {
            log.info("Rate limit cleanup: Removed {} expired entries", deleted);
        }
    }

    /**
     * Get current count of blocked IPs.
     * Used for monitoring.
     *
     * @return Number of currently blocked IPs
     */
    @Transactional(readOnly = true)
    public long getBlockedIpCount() {
        return rateLimitRepository.countBlockedIps(LocalDateTime.now());
    }
}
