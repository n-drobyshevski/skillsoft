package app.skillsoft.assessmentbackend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for SessionTokenService.
 *
 * Tests cover:
 * - Token generation (256-bit, Base64 URL-safe encoding)
 * - Token hashing (SHA-256, hex encoding)
 * - Token validation (constant-time comparison, null handling)
 * - TokenWithHash record construction
 * - Security properties (randomness, uniqueness)
 */
@DisplayName("SessionTokenService Tests")
class SessionTokenServiceTest {

    private SessionTokenService sessionTokenService;

    /**
     * Pattern for valid Base64 URL-safe encoded strings without padding.
     * Allows alphanumeric characters plus '-' and '_'.
     */
    private static final Pattern BASE64_URL_PATTERN = Pattern.compile("^[A-Za-z0-9_-]+$");

    /**
     * Pattern for valid hex-encoded strings (lowercase).
     */
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-f]+$");

    /**
     * Expected length of Base64 URL-safe encoded 256-bit token without padding.
     * 32 bytes * 8 bits / 6 bits per Base64 char = 42.67, rounded up = 43 chars.
     */
    private static final int EXPECTED_TOKEN_LENGTH = 43;

    /**
     * Expected length of SHA-256 hex-encoded hash.
     * 256 bits / 4 bits per hex char = 64 chars.
     */
    private static final int EXPECTED_HASH_LENGTH = 64;

    @BeforeEach
    void setUp() {
        sessionTokenService = new SessionTokenService();
    }

    @Nested
    @DisplayName("generateToken")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate token with correct length (43 characters)")
        void shouldGenerateTokenWithCorrectLength() {
            // When
            String token = sessionTokenService.generateToken();

            // Then
            assertThat(token).hasSize(EXPECTED_TOKEN_LENGTH);
        }

        @Test
        @DisplayName("Should generate Base64 URL-safe encoded token")
        void shouldGenerateBase64UrlSafeEncodedToken() {
            // When
            String token = sessionTokenService.generateToken();

            // Then
            assertThat(token).matches(BASE64_URL_PATTERN);
        }

        @Test
        @DisplayName("Should generate token that can be decoded to 32 bytes")
        void shouldGenerateTokenDecodableTo32Bytes() {
            // When
            String token = sessionTokenService.generateToken();
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token);

            // Then
            assertThat(decodedBytes).hasSize(32);
        }

        @Test
        @DisplayName("Should generate unique tokens")
        void shouldGenerateUniqueTokens() {
            // Given
            Set<String> tokens = new HashSet<>();
            int numberOfTokens = 100;

            // When
            for (int i = 0; i < numberOfTokens; i++) {
                tokens.add(sessionTokenService.generateToken());
            }

            // Then
            assertThat(tokens).hasSize(numberOfTokens);
        }

        @RepeatedTest(10)
        @DisplayName("Should consistently generate valid tokens")
        void shouldConsistentlyGenerateValidTokens() {
            // When
            String token = sessionTokenService.generateToken();

            // Then
            assertThat(token)
                    .isNotNull()
                    .isNotBlank()
                    .hasSize(EXPECTED_TOKEN_LENGTH)
                    .matches(BASE64_URL_PATTERN);
        }

        @Test
        @DisplayName("Should generate token without padding characters")
        void shouldGenerateTokenWithoutPadding() {
            // When
            String token = sessionTokenService.generateToken();

            // Then
            assertThat(token).doesNotContain("=");
        }

        @Test
        @DisplayName("Should generate token without standard Base64 characters + and /")
        void shouldGenerateTokenWithoutStandardBase64Characters() {
            // Given - generate multiple tokens to increase chance of detecting issues
            Set<String> tokens = new HashSet<>();
            for (int i = 0; i < 100; i++) {
                tokens.add(sessionTokenService.generateToken());
            }

            // Then
            for (String token : tokens) {
                assertThat(token)
                        .doesNotContain("+")
                        .doesNotContain("/");
            }
        }
    }

    @Nested
    @DisplayName("hashToken")
    class HashTokenTests {

        @Test
        @DisplayName("Should hash token to 64 character hex string")
        void shouldHashTokenTo64CharHexString() {
            // Given
            String token = sessionTokenService.generateToken();

            // When
            String hash = sessionTokenService.hashToken(token);

            // Then
            assertThat(hash).hasSize(EXPECTED_HASH_LENGTH);
        }

        @Test
        @DisplayName("Should produce lowercase hex-encoded hash")
        void shouldProduceLowercaseHexEncodedHash() {
            // Given
            String token = sessionTokenService.generateToken();

            // When
            String hash = sessionTokenService.hashToken(token);

            // Then
            assertThat(hash).matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should produce consistent hash for same token")
        void shouldProduceConsistentHashForSameToken() {
            // Given
            String token = sessionTokenService.generateToken();

            // When
            String hash1 = sessionTokenService.hashToken(token);
            String hash2 = sessionTokenService.hashToken(token);

            // Then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("Should produce different hashes for different tokens")
        void shouldProduceDifferentHashesForDifferentTokens() {
            // Given
            String token1 = sessionTokenService.generateToken();
            String token2 = sessionTokenService.generateToken();

            // When
            String hash1 = sessionTokenService.hashToken(token1);
            String hash2 = sessionTokenService.hashToken(token2);

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("Should produce correct SHA-256 hash for known input")
        void shouldProduceCorrectSha256HashForKnownInput() throws NoSuchAlgorithmException {
            // Given - a known test string
            String testInput = "test-token-12345";

            // Calculate expected hash independently
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedHashBytes = digest.digest(testInput.getBytes(StandardCharsets.UTF_8));
            String expectedHash = HexFormat.of().formatHex(expectedHashBytes);

            // When
            String actualHash = sessionTokenService.hashToken(testInput);

            // Then
            assertThat(actualHash).isEqualTo(expectedHash);
        }

        @Test
        @DisplayName("Should handle empty string")
        void shouldHandleEmptyString() {
            // When
            String hash = sessionTokenService.hashToken("");

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should handle Cyrillic characters in token")
        void shouldHandleCyrillicCharactersInToken() {
            // Given - Russian text
            String cyrillicToken = "токен-сессии-тест";

            // When
            String hash = sessionTokenService.hashToken(cyrillicToken);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should handle special characters in token")
        void shouldHandleSpecialCharactersInToken() {
            // Given
            String specialToken = "token!@#$%^&*()_+-=[]{}|;':\",./<>?";

            // When
            String hash = sessionTokenService.hashToken(specialToken);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should handle very long token")
        void shouldHandleVeryLongToken() {
            // Given - a very long string
            String longToken = "x".repeat(10000);

            // When
            String hash = sessionTokenService.hashToken(longToken);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }
    }

    @Nested
    @DisplayName("validateToken")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid token and matching hash")
        void shouldReturnTrueForValidTokenAndMatchingHash() {
            // Given
            String token = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(token);

            // When
            boolean isValid = sessionTokenService.validateToken(token, hash);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for token with non-matching hash")
        void shouldReturnFalseForTokenWithNonMatchingHash() {
            // Given
            String token = sessionTokenService.generateToken();
            String differentToken = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(differentToken);

            // When
            boolean isValid = sessionTokenService.validateToken(token, hash);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false when token is null")
        void shouldReturnFalseWhenTokenIsNull() {
            // Given
            String hash = sessionTokenService.hashToken("some-token");

            // When
            boolean isValid = sessionTokenService.validateToken(null, hash);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false when stored hash is null")
        void shouldReturnFalseWhenStoredHashIsNull() {
            // Given
            String token = sessionTokenService.generateToken();

            // When
            boolean isValid = sessionTokenService.validateToken(token, null);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false when both token and hash are null")
        void shouldReturnFalseWhenBothTokenAndHashAreNull() {
            // When
            boolean isValid = sessionTokenService.validateToken(null, null);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty token")
        void shouldReturnFalseForEmptyToken() {
            // Given
            String validToken = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(validToken);

            // When
            boolean isValid = sessionTokenService.validateToken("", hash);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for modified token")
        void shouldReturnFalseForModifiedToken() {
            // Given
            String token = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(token);
            String modifiedToken = token.substring(0, token.length() - 1) + "X";

            // When
            boolean isValid = sessionTokenService.validateToken(modifiedToken, hash);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for uppercase hash comparison")
        void shouldReturnFalseForUppercaseHashComparison() {
            // Given
            String token = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(token);
            String uppercaseHash = hash.toUpperCase();

            // When
            boolean isValid = sessionTokenService.validateToken(token, uppercaseHash);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should handle validation with manually computed hash")
        void shouldHandleValidationWithManuallyComputedHash() throws NoSuchAlgorithmException {
            // Given
            String token = "manual-test-token";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            String manualHash = HexFormat.of().formatHex(hashBytes);

            // When
            boolean isValid = sessionTokenService.validateToken(token, manualHash);

            // Then
            assertThat(isValid).isTrue();
        }

        @RepeatedTest(10)
        @DisplayName("Should consistently validate tokens correctly")
        void shouldConsistentlyValidateTokensCorrectly() {
            // Given
            String token = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(token);

            // When & Then
            assertThat(sessionTokenService.validateToken(token, hash)).isTrue();
            assertThat(sessionTokenService.validateToken(token + "x", hash)).isFalse();
        }
    }

    @Nested
    @DisplayName("generateTokenWithHash")
    class GenerateTokenWithHashTests {

        @Test
        @DisplayName("Should return TokenWithHash record with valid token")
        void shouldReturnTokenWithHashRecordWithValidToken() {
            // When
            SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.token())
                    .isNotNull()
                    .hasSize(EXPECTED_TOKEN_LENGTH)
                    .matches(BASE64_URL_PATTERN);
        }

        @Test
        @DisplayName("Should return TokenWithHash record with valid hash")
        void shouldReturnTokenWithHashRecordWithValidHash() {
            // When
            SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.hash())
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should return TokenWithHash where hash matches token")
        void shouldReturnTokenWithHashWhereHashMatchesToken() {
            // When
            SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();

            // Then
            String recomputedHash = sessionTokenService.hashToken(result.token());
            assertThat(result.hash()).isEqualTo(recomputedHash);
        }

        @Test
        @DisplayName("Should return TokenWithHash that validates correctly")
        void shouldReturnTokenWithHashThatValidatesCorrectly() {
            // When
            SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();

            // Then
            boolean isValid = sessionTokenService.validateToken(result.token(), result.hash());
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should generate unique TokenWithHash instances")
        void shouldGenerateUniqueTokenWithHashInstances() {
            // Given
            Set<String> tokens = new HashSet<>();
            Set<String> hashes = new HashSet<>();
            int numberOfPairs = 50;

            // When
            for (int i = 0; i < numberOfPairs; i++) {
                SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();
                tokens.add(result.token());
                hashes.add(result.hash());
            }

            // Then
            assertThat(tokens).hasSize(numberOfPairs);
            assertThat(hashes).hasSize(numberOfPairs);
        }

        @RepeatedTest(10)
        @DisplayName("Should consistently generate valid TokenWithHash")
        void shouldConsistentlyGenerateValidTokenWithHash() {
            // When
            SessionTokenService.TokenWithHash result = sessionTokenService.generateTokenWithHash();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.token()).isNotNull().isNotBlank();
            assertThat(result.hash()).isNotNull().isNotBlank();
            assertThat(sessionTokenService.validateToken(result.token(), result.hash())).isTrue();
        }
    }

    @Nested
    @DisplayName("TokenWithHash Record")
    class TokenWithHashRecordTests {

        @Test
        @DisplayName("Should create TokenWithHash with provided values")
        void shouldCreateTokenWithHashWithProvidedValues() {
            // Given
            String token = "test-token";
            String hash = "test-hash";

            // When
            SessionTokenService.TokenWithHash record = new SessionTokenService.TokenWithHash(token, hash);

            // Then
            assertThat(record.token()).isEqualTo(token);
            assertThat(record.hash()).isEqualTo(hash);
        }

        @Test
        @DisplayName("Should support record equality")
        void shouldSupportRecordEquality() {
            // Given
            String token = "test-token";
            String hash = "test-hash";

            // When
            SessionTokenService.TokenWithHash record1 = new SessionTokenService.TokenWithHash(token, hash);
            SessionTokenService.TokenWithHash record2 = new SessionTokenService.TokenWithHash(token, hash);

            // Then
            assertThat(record1).isEqualTo(record2);
            assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
        }

        @Test
        @DisplayName("Should not be equal with different token")
        void shouldNotBeEqualWithDifferentToken() {
            // Given
            String hash = "same-hash";

            // When
            SessionTokenService.TokenWithHash record1 = new SessionTokenService.TokenWithHash("token1", hash);
            SessionTokenService.TokenWithHash record2 = new SessionTokenService.TokenWithHash("token2", hash);

            // Then
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should not be equal with different hash")
        void shouldNotBeEqualWithDifferentHash() {
            // Given
            String token = "same-token";

            // When
            SessionTokenService.TokenWithHash record1 = new SessionTokenService.TokenWithHash(token, "hash1");
            SessionTokenService.TokenWithHash record2 = new SessionTokenService.TokenWithHash(token, "hash2");

            // Then
            assertThat(record1).isNotEqualTo(record2);
        }

        @Test
        @DisplayName("Should have meaningful toString")
        void shouldHaveMeaningfulToString() {
            // Given
            SessionTokenService.TokenWithHash record = new SessionTokenService.TokenWithHash("token", "hash");

            // When
            String toString = record.toString();

            // Then
            assertThat(toString)
                    .contains("TokenWithHash")
                    .contains("token")
                    .contains("hash");
        }
    }

    @Nested
    @DisplayName("Security Properties")
    class SecurityPropertiesTests {

        @Test
        @DisplayName("Should use cryptographically secure random generation")
        void shouldUseCryptographicallySecureRandomGeneration() {
            // Given - generate a large number of tokens
            Set<String> tokens = new HashSet<>();
            int numberOfTokens = 1000;

            // When
            for (int i = 0; i < numberOfTokens; i++) {
                tokens.add(sessionTokenService.generateToken());
            }

            // Then - all tokens should be unique (no collisions with secure random)
            assertThat(tokens).hasSize(numberOfTokens);
        }

        @Test
        @DisplayName("Should produce 256 bits of entropy in tokens")
        void shouldProduce256BitsOfEntropyInTokens() {
            // When
            String token = sessionTokenService.generateToken();
            byte[] decodedBytes = Base64.getUrlDecoder().decode(token);

            // Then - 256 bits = 32 bytes
            assertThat(decodedBytes.length * 8).isEqualTo(256);
        }

        @Test
        @DisplayName("Should not expose original token through hash")
        void shouldNotExposeOriginalTokenThroughHash() {
            // Given
            String token = sessionTokenService.generateToken();

            // When
            String hash = sessionTokenService.hashToken(token);

            // Then - hash should not contain the token
            assertThat(hash).doesNotContain(token);
            // Hash length is fixed regardless of input length
            assertThat(hash).hasSize(EXPECTED_HASH_LENGTH);
        }

        @Test
        @DisplayName("Should produce different hashes for similar tokens")
        void shouldProduceDifferentHashesForSimilarTokens() {
            // Given - tokens that differ by only one character
            String token1 = "abcdefghijklmnopqrstuvwxyz123456";
            String token2 = "abcdefghijklmnopqrstuvwxyz123457";

            // When
            String hash1 = sessionTokenService.hashToken(token1);
            String hash2 = sessionTokenService.hashToken(token2);

            // Then - hashes should be completely different (avalanche effect)
            assertThat(hash1).isNotEqualTo(hash2);

            // Count different characters
            int differentChars = 0;
            for (int i = 0; i < hash1.length(); i++) {
                if (hash1.charAt(i) != hash2.charAt(i)) {
                    differentChars++;
                }
            }
            // Good hash should change approximately half the bits on average
            assertThat(differentChars).isGreaterThan(10);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle concurrent token generation")
        void shouldHandleConcurrentTokenGeneration() throws InterruptedException {
            // Given
            Set<String> tokens = java.util.Collections.synchronizedSet(new HashSet<>());
            int numberOfThreads = 10;
            int tokensPerThread = 100;
            Thread[] threads = new Thread[numberOfThreads];

            // When
            for (int i = 0; i < numberOfThreads; i++) {
                threads[i] = new Thread(() -> {
                    for (int j = 0; j < tokensPerThread; j++) {
                        tokens.add(sessionTokenService.generateToken());
                    }
                });
                threads[i].start();
            }

            for (Thread thread : threads) {
                thread.join();
            }

            // Then - all tokens should be unique
            assertThat(tokens).hasSize(numberOfThreads * tokensPerThread);
        }

        @Test
        @DisplayName("Should handle whitespace-only token for hashing")
        void shouldHandleWhitespaceOnlyTokenForHashing() {
            // Given
            String whitespaceToken = "   \t\n  ";

            // When
            String hash = sessionTokenService.hashToken(whitespaceToken);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should handle unicode characters in token")
        void shouldHandleUnicodeCharactersInToken() {
            // Given - various unicode characters
            String unicodeToken = "token-\u00e9\u00e8\u00ea-\u4e2d\u6587-\ud83d\ude00";

            // When
            String hash = sessionTokenService.hashToken(unicodeToken);

            // Then
            assertThat(hash)
                    .isNotNull()
                    .hasSize(EXPECTED_HASH_LENGTH)
                    .matches(HEX_PATTERN);
        }

        @Test
        @DisplayName("Should validate token with leading/trailing spaces as different")
        void shouldValidateTokenWithSpacesAsDifferent() {
            // Given
            String token = sessionTokenService.generateToken();
            String hash = sessionTokenService.hashToken(token);

            // When & Then
            assertThat(sessionTokenService.validateToken(" " + token, hash)).isFalse();
            assertThat(sessionTokenService.validateToken(token + " ", hash)).isFalse();
            assertThat(sessionTokenService.validateToken(" " + token + " ", hash)).isFalse();
        }
    }
}
