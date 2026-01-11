package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.domain.entities.AnonymousSessionRateLimit;
import app.skillsoft.assessmentbackend.exception.RateLimitExceededException;
import app.skillsoft.assessmentbackend.repository.AnonymousSessionRateLimitRepository;
import app.skillsoft.assessmentbackend.testutils.BaseUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitService implementation.
 *
 * Tests cover:
 * - Rate limit checking (under limit, exceeded, blocked)
 * - Remaining allowed sessions calculation
 * - IP blocking status checks
 * - Seconds until unblocked calculation
 * - Manual IP unblocking
 * - Scheduled cleanup of expired entries
 * - Blocked IP count monitoring
 * - Edge cases: null/blank/empty IP addresses
 * - Different IP addresses have separate limits
 *
 * @author SkillSoft Development Team
 */
@DisplayName("RateLimitService Tests")
class RateLimitServiceTest extends BaseUnitTest {

    @Mock
    private AnonymousSessionRateLimitRepository rateLimitRepository;

    private RateLimitService rateLimitService;

    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_IP_2 = "10.0.0.50";
    private static final String TEST_IPV6 = "2001:0db8:85a3:0000:0000:8a2e:0370:7334";

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitService(rateLimitRepository);
    }

    // ========================================
    // checkRateLimit Tests
    // ========================================

    @Nested
    @DisplayName("checkRateLimit")
    class CheckRateLimitTests {

        @Test
        @DisplayName("Should allow request when IP is null")
        void shouldAllowWhenIpIsNull() {
            // When & Then - should not throw
            assertThatCode(() -> rateLimitService.checkRateLimit(null))
                    .doesNotThrowAnyException();

            // Verify repository was never called
            verify(rateLimitRepository, never()).findByIpAddress(anyString());
            verify(rateLimitRepository, never()).save(any(AnonymousSessionRateLimit.class));
        }

        @Test
        @DisplayName("Should allow request when IP is empty string")
        void shouldAllowWhenIpIsEmpty() {
            // When & Then
            assertThatCode(() -> rateLimitService.checkRateLimit(""))
                    .doesNotThrowAnyException();

            verify(rateLimitRepository, never()).findByIpAddress(anyString());
        }

        @Test
        @DisplayName("Should allow request when IP is blank (whitespace only)")
        void shouldAllowWhenIpIsBlank() {
            // When & Then
            assertThatCode(() -> rateLimitService.checkRateLimit("   "))
                    .doesNotThrowAnyException();

            verify(rateLimitRepository, never()).findByIpAddress(anyString());
        }

        @Test
        @DisplayName("Should create new rate limit entry for first request from IP")
        void shouldCreateNewEntryForFirstRequest() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.empty());
            ArgumentCaptor<AnonymousSessionRateLimit> captor = ArgumentCaptor.forClass(AnonymousSessionRateLimit.class);

            // When
            rateLimitService.checkRateLimit(TEST_IP);

            // Then
            verify(rateLimitRepository).findByIpAddress(TEST_IP);
            verify(rateLimitRepository).save(captor.capture());

            AnonymousSessionRateLimit saved = captor.getValue();
            assertThat(saved.getIpAddress()).isEqualTo(TEST_IP);
            assertThat(saved.getSessionCount()).isEqualTo(1);
            assertThat(saved.getWindowStart()).isNotNull();
            assertThat(saved.getBlockedUntil()).isNull();
        }

        @Test
        @DisplayName("Should increment count when under rate limit")
        void shouldIncrementCountWhenUnderLimit() {
            // Given
            AnonymousSessionRateLimit existingLimit = createRateLimitEntry(TEST_IP, 5);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(existingLimit));

            // When
            rateLimitService.checkRateLimit(TEST_IP);

            // Then
            verify(rateLimitRepository).save(existingLimit);
            // incrementAndCheck increases count by 1, so 5 -> 6
            assertThat(existingLimit.getSessionCount()).isEqualTo(6);
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException when limit is reached")
        void shouldThrowWhenLimitReached() {
            // Given - count is 9, after increment becomes 10, which triggers block
            AnonymousSessionRateLimit existingLimit = createRateLimitEntry(TEST_IP, 9);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(existingLimit));

            // When & Then
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class);

            verify(rateLimitRepository).save(existingLimit);
            assertThat(existingLimit.getBlockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException when IP is already blocked")
        void shouldThrowWhenIpIsBlocked() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When & Then
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class);

            // Should not save when already blocked
            verify(rateLimitRepository, never()).save(any(AnonymousSessionRateLimit.class));
        }

        @Test
        @DisplayName("Should include retryAfterSeconds in RateLimitExceededException")
        void shouldIncludeRetryAfterInException() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When & Then
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(ex -> {
                        RateLimitExceededException rlEx = (RateLimitExceededException) ex;
                        assertThat(rlEx.getRetryAfterSeconds()).isGreaterThan(0);
                    });
        }

        @Test
        @DisplayName("Should handle IPv6 addresses correctly")
        void shouldHandleIpv6Addresses() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IPV6)).thenReturn(Optional.empty());
            ArgumentCaptor<AnonymousSessionRateLimit> captor = ArgumentCaptor.forClass(AnonymousSessionRateLimit.class);

            // When
            rateLimitService.checkRateLimit(TEST_IPV6);

            // Then
            verify(rateLimitRepository).save(captor.capture());
            assertThat(captor.getValue().getIpAddress()).isEqualTo(TEST_IPV6);
        }

        @Test
        @DisplayName("Should maintain separate limits for different IP addresses")
        void shouldMaintainSeparateLimitsForDifferentIps() {
            // Given
            AnonymousSessionRateLimit limit1 = createRateLimitEntry(TEST_IP, 3);
            AnonymousSessionRateLimit limit2 = createRateLimitEntry(TEST_IP_2, 7);

            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit1));
            when(rateLimitRepository.findByIpAddress(TEST_IP_2)).thenReturn(Optional.of(limit2));

            // When
            rateLimitService.checkRateLimit(TEST_IP);
            rateLimitService.checkRateLimit(TEST_IP_2);

            // Then
            verify(rateLimitRepository).findByIpAddress(TEST_IP);
            verify(rateLimitRepository).findByIpAddress(TEST_IP_2);
            verify(rateLimitRepository, times(2)).save(any(AnonymousSessionRateLimit.class));

            // Verify counts incremented independently
            assertThat(limit1.getSessionCount()).isEqualTo(4);
            assertThat(limit2.getSessionCount()).isEqualTo(8);
        }

        @Test
        @DisplayName("Should allow request after window expires")
        void shouldAllowAfterWindowExpires() {
            // Given - create entry with expired window
            AnonymousSessionRateLimit expiredLimit = createExpiredWindowRateLimitEntry(TEST_IP, 9);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(expiredLimit));

            // When - should not throw because window is expired and will reset
            assertThatCode(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .doesNotThrowAnyException();

            // Then
            verify(rateLimitRepository).save(expiredLimit);
            // After window reset, count should be 1 (resetWindow sets to 0, then increment makes it 1)
            assertThat(expiredLimit.getSessionCount()).isEqualTo(1);
        }
    }

    // ========================================
    // getRemainingAllowed Tests
    // ========================================

    @Nested
    @DisplayName("getRemainingAllowed")
    class GetRemainingAllowedTests {

        @Test
        @DisplayName("Should return max sessions when IP is null")
        void shouldReturnMaxWhenIpIsNull() {
            // When
            int remaining = rateLimitService.getRemainingAllowed(null);

            // Then
            assertThat(remaining).isEqualTo(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
            verify(rateLimitRepository, never()).findByIpAddress(anyString());
        }

        @Test
        @DisplayName("Should return max sessions when IP is blank")
        void shouldReturnMaxWhenIpIsBlank() {
            // When
            int remaining = rateLimitService.getRemainingAllowed("  ");

            // Then
            assertThat(remaining).isEqualTo(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
            verify(rateLimitRepository, never()).findByIpAddress(anyString());
        }

        @Test
        @DisplayName("Should return max sessions when IP is not tracked")
        void shouldReturnMaxWhenIpNotTracked() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.empty());

            // When
            int remaining = rateLimitService.getRemainingAllowed(TEST_IP);

            // Then
            assertThat(remaining).isEqualTo(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
        }

        @Test
        @DisplayName("Should return correct remaining count for tracked IP")
        void shouldReturnCorrectRemainingForTrackedIp() {
            // Given - 4 sessions used out of 10
            AnonymousSessionRateLimit limit = createRateLimitEntry(TEST_IP, 4);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When
            int remaining = rateLimitService.getRemainingAllowed(TEST_IP);

            // Then
            assertThat(remaining).isEqualTo(6); // 10 - 4 = 6
        }

        @Test
        @DisplayName("Should return zero when limit is reached")
        void shouldReturnZeroWhenLimitReached() {
            // Given
            AnonymousSessionRateLimit limit = createRateLimitEntry(TEST_IP, 10);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When
            int remaining = rateLimitService.getRemainingAllowed(TEST_IP);

            // Then
            assertThat(remaining).isEqualTo(0);
        }

        @Test
        @DisplayName("Should return zero when IP is blocked")
        void shouldReturnZeroWhenIpIsBlocked() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When
            int remaining = rateLimitService.getRemainingAllowed(TEST_IP);

            // Then
            assertThat(remaining).isEqualTo(0);
        }
    }

    // ========================================
    // isBlocked Tests
    // ========================================

    @Nested
    @DisplayName("isBlocked")
    class IsBlockedTests {

        @Test
        @DisplayName("Should return false when IP is null")
        void shouldReturnFalseWhenIpIsNull() {
            // When
            boolean blocked = rateLimitService.isBlocked(null);

            // Then
            assertThat(blocked).isFalse();
            verify(rateLimitRepository, never()).findByIpAddress(anyString());
        }

        @Test
        @DisplayName("Should return false when IP is blank")
        void shouldReturnFalseWhenIpIsBlank() {
            // When
            boolean blocked = rateLimitService.isBlocked("");

            // Then
            assertThat(blocked).isFalse();
        }

        @Test
        @DisplayName("Should return false when IP is not tracked")
        void shouldReturnFalseWhenIpNotTracked() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.empty());

            // When
            boolean blocked = rateLimitService.isBlocked(TEST_IP);

            // Then
            assertThat(blocked).isFalse();
        }

        @Test
        @DisplayName("Should return false when IP is tracked but not blocked")
        void shouldReturnFalseWhenNotBlocked() {
            // Given
            AnonymousSessionRateLimit limit = createRateLimitEntry(TEST_IP, 5);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When
            boolean blocked = rateLimitService.isBlocked(TEST_IP);

            // Then
            assertThat(blocked).isFalse();
        }

        @Test
        @DisplayName("Should return true when IP is blocked")
        void shouldReturnTrueWhenBlocked() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When
            boolean blocked = rateLimitService.isBlocked(TEST_IP);

            // Then
            assertThat(blocked).isTrue();
        }

        @Test
        @DisplayName("Should return false when block has expired")
        void shouldReturnFalseWhenBlockExpired() {
            // Given - create entry with expired block
            AnonymousSessionRateLimit expiredBlock = createExpiredBlockRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(expiredBlock));

            // When
            boolean blocked = rateLimitService.isBlocked(TEST_IP);

            // Then
            assertThat(blocked).isFalse();
        }
    }

    // ========================================
    // getSecondsUntilUnblocked Tests
    // ========================================

    @Nested
    @DisplayName("getSecondsUntilUnblocked")
    class GetSecondsUntilUnblockedTests {

        @Test
        @DisplayName("Should return 0 when IP is null")
        void shouldReturnZeroWhenIpIsNull() {
            // When
            long seconds = rateLimitService.getSecondsUntilUnblocked(null);

            // Then
            assertThat(seconds).isZero();
        }

        @Test
        @DisplayName("Should return 0 when IP is blank")
        void shouldReturnZeroWhenIpIsBlank() {
            // When
            long seconds = rateLimitService.getSecondsUntilUnblocked("  ");

            // Then
            assertThat(seconds).isZero();
        }

        @Test
        @DisplayName("Should return 0 when IP is not tracked")
        void shouldReturnZeroWhenIpNotTracked() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.empty());

            // When
            long seconds = rateLimitService.getSecondsUntilUnblocked(TEST_IP);

            // Then
            assertThat(seconds).isZero();
        }

        @Test
        @DisplayName("Should return 0 when IP is not blocked")
        void shouldReturnZeroWhenNotBlocked() {
            // Given
            AnonymousSessionRateLimit limit = createRateLimitEntry(TEST_IP, 5);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When
            long seconds = rateLimitService.getSecondsUntilUnblocked(TEST_IP);

            // Then
            assertThat(seconds).isZero();
        }

        @Test
        @DisplayName("Should return positive seconds when IP is blocked")
        void shouldReturnPositiveSecondsWhenBlocked() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When
            long seconds = rateLimitService.getSecondsUntilUnblocked(TEST_IP);

            // Then
            assertThat(seconds).isGreaterThan(0);
            // Block is for 1 hour, so should be close to 3600 seconds (allowing some tolerance)
            assertThat(seconds).isLessThanOrEqualTo(3600);
        }
    }

    // ========================================
    // unblock Tests
    // ========================================

    @Nested
    @DisplayName("unblock")
    class UnblockTests {

        @Test
        @DisplayName("Should return false when IP is not found")
        void shouldReturnFalseWhenIpNotFound() {
            // Given
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.empty());

            // When
            boolean result = rateLimitService.unblock(TEST_IP);

            // Then
            assertThat(result).isFalse();
            verify(rateLimitRepository, never()).save(any(AnonymousSessionRateLimit.class));
        }

        @Test
        @DisplayName("Should unblock IP and reset window when found")
        void shouldUnblockAndResetWindowWhenFound() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When
            boolean result = rateLimitService.unblock(TEST_IP);

            // Then
            assertThat(result).isTrue();
            verify(rateLimitRepository).save(blockedLimit);
            assertThat(blockedLimit.getBlockedUntil()).isNull();
            assertThat(blockedLimit.getSessionCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should reset non-blocked entry when found")
        void shouldResetNonBlockedEntryWhenFound() {
            // Given
            AnonymousSessionRateLimit limit = createRateLimitEntry(TEST_IP, 5);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When
            boolean result = rateLimitService.unblock(TEST_IP);

            // Then
            assertThat(result).isTrue();
            verify(rateLimitRepository).save(limit);
            assertThat(limit.getSessionCount()).isEqualTo(0);
        }
    }

    // ========================================
    // cleanupExpiredEntries Tests
    // ========================================

    @Nested
    @DisplayName("cleanupExpiredEntries")
    class CleanupExpiredEntriesTests {

        @Test
        @DisplayName("Should call repository deleteStaleEntries with correct cutoffs")
        void shouldCallDeleteStaleEntriesWithCorrectCutoffs() {
            // Given
            when(rateLimitRepository.deleteStaleEntries(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(5);

            // When
            rateLimitService.cleanupExpiredEntries();

            // Then
            ArgumentCaptor<LocalDateTime> windowCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> blockCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            verify(rateLimitRepository).deleteStaleEntries(
                    windowCutoffCaptor.capture(),
                    blockCutoffCaptor.capture()
            );

            LocalDateTime windowCutoff = windowCutoffCaptor.getValue();
            LocalDateTime blockCutoff = blockCutoffCaptor.getValue();

            // Window cutoff should be 24 hours ago (with some tolerance)
            assertThat(windowCutoff).isBefore(LocalDateTime.now().minusHours(23));
            assertThat(windowCutoff).isAfter(LocalDateTime.now().minusHours(25));

            // Block cutoff should be around now
            assertThat(blockCutoff).isBetween(
                    LocalDateTime.now().minusSeconds(5),
                    LocalDateTime.now().plusSeconds(5)
            );
        }

        @Test
        @DisplayName("Should handle zero deleted entries gracefully")
        void shouldHandleZeroDeletedEntriesGracefully() {
            // Given
            when(rateLimitRepository.deleteStaleEntries(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(0);

            // When & Then - should not throw
            assertThatCode(() -> rateLimitService.cleanupExpiredEntries())
                    .doesNotThrowAnyException();

            verify(rateLimitRepository).deleteStaleEntries(any(LocalDateTime.class), any(LocalDateTime.class));
        }
    }

    // ========================================
    // getBlockedIpCount Tests
    // ========================================

    @Nested
    @DisplayName("getBlockedIpCount")
    class GetBlockedIpCountTests {

        @Test
        @DisplayName("Should return count from repository")
        void shouldReturnCountFromRepository() {
            // Given
            when(rateLimitRepository.countBlockedIps(any(LocalDateTime.class))).thenReturn(7L);

            // When
            long count = rateLimitService.getBlockedIpCount();

            // Then
            assertThat(count).isEqualTo(7);
            verify(rateLimitRepository).countBlockedIps(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should return zero when no blocked IPs")
        void shouldReturnZeroWhenNoBlockedIps() {
            // Given
            when(rateLimitRepository.countBlockedIps(any(LocalDateTime.class))).thenReturn(0L);

            // When
            long count = rateLimitService.getBlockedIpCount();

            // Then
            assertThat(count).isZero();
        }
    }

    // ========================================
    // Integration-like Scenarios
    // ========================================

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarioTests {

        @Test
        @DisplayName("Should block IP after 10 consecutive requests")
        void shouldBlockAfterTenConsecutiveRequests() {
            // Given
            AnonymousSessionRateLimit limit = new AnonymousSessionRateLimit(TEST_IP);
            // Simulate 9 previous requests
            for (int i = 0; i < 8; i++) {
                limit.incrementAndCheck();
            }
            // Now at 9 sessions

            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(limit));

            // When - 10th request should trigger block
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class);

            // Then
            assertThat(limit.isBlocked()).isTrue();
            assertThat(limit.getBlockedUntil()).isNotNull();
        }

        @Test
        @DisplayName("Should allow requests from new IP while another is blocked")
        void shouldAllowNewIpWhileAnotherIsBlocked() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));
            when(rateLimitRepository.findByIpAddress(TEST_IP_2)).thenReturn(Optional.empty());

            // When - blocked IP should fail
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class);

            // And new IP should succeed
            assertThatCode(() -> rateLimitService.checkRateLimit(TEST_IP_2))
                    .doesNotThrowAnyException();

            // Then
            verify(rateLimitRepository).save(argThat(rl -> rl.getIpAddress().equals(TEST_IP_2)));
        }

        @Test
        @DisplayName("Should provide bilingual error messages in exception")
        void shouldProvideBilingualErrorMessages() {
            // Given
            AnonymousSessionRateLimit blockedLimit = createBlockedRateLimitEntry(TEST_IP);
            when(rateLimitRepository.findByIpAddress(TEST_IP)).thenReturn(Optional.of(blockedLimit));

            // When & Then
            assertThatThrownBy(() -> rateLimitService.checkRateLimit(TEST_IP))
                    .isInstanceOf(RateLimitExceededException.class)
                    .satisfies(ex -> {
                        RateLimitExceededException rlEx = (RateLimitExceededException) ex;
                        assertThat(rlEx.getMessage()).isNotBlank();
                        assertThat(rlEx.getMessageRu()).isNotBlank();
                        // Verify Russian message contains Cyrillic characters
                        assertThat(rlEx.getMessageRu()).containsPattern("[\\u0400-\\u04FF]");
                    });
        }
    }

    // ========================================
    // Helper Methods
    // ========================================

    /**
     * Create a rate limit entry with the specified session count.
     * Window start is set to now.
     */
    private AnonymousSessionRateLimit createRateLimitEntry(String ipAddress, int sessionCount) {
        AnonymousSessionRateLimit limit = new AnonymousSessionRateLimit();
        limit.setId(UUID.randomUUID());
        limit.setIpAddress(ipAddress);
        limit.setSessionCount(sessionCount);
        limit.setWindowStart(LocalDateTime.now());
        limit.setBlockedUntil(null);
        return limit;
    }

    /**
     * Create a blocked rate limit entry.
     * Blocked until 1 hour from now.
     */
    private AnonymousSessionRateLimit createBlockedRateLimitEntry(String ipAddress) {
        AnonymousSessionRateLimit limit = new AnonymousSessionRateLimit();
        limit.setId(UUID.randomUUID());
        limit.setIpAddress(ipAddress);
        limit.setSessionCount(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
        limit.setWindowStart(LocalDateTime.now().minusMinutes(30));
        limit.setBlockedUntil(LocalDateTime.now().plusHours(1));
        return limit;
    }

    /**
     * Create a rate limit entry with an expired block.
     * Block expired 1 hour ago.
     */
    private AnonymousSessionRateLimit createExpiredBlockRateLimitEntry(String ipAddress) {
        AnonymousSessionRateLimit limit = new AnonymousSessionRateLimit();
        limit.setId(UUID.randomUUID());
        limit.setIpAddress(ipAddress);
        limit.setSessionCount(AnonymousSessionRateLimit.MAX_SESSIONS_PER_WINDOW);
        limit.setWindowStart(LocalDateTime.now().minusHours(2));
        limit.setBlockedUntil(LocalDateTime.now().minusHours(1)); // Block expired
        return limit;
    }

    /**
     * Create a rate limit entry with an expired window.
     * Window started 2 hours ago (beyond the 1 hour window).
     */
    private AnonymousSessionRateLimit createExpiredWindowRateLimitEntry(String ipAddress, int sessionCount) {
        AnonymousSessionRateLimit limit = new AnonymousSessionRateLimit();
        limit.setId(UUID.randomUUID());
        limit.setIpAddress(ipAddress);
        limit.setSessionCount(sessionCount);
        limit.setWindowStart(LocalDateTime.now().minusHours(2)); // Expired window
        limit.setBlockedUntil(null);
        return limit;
    }
}
