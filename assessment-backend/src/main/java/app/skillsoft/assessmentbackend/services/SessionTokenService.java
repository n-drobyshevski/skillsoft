package app.skillsoft.assessmentbackend.services;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for generating and hashing session access tokens.
 *
 * <p>Session tokens are used to authenticate anonymous test sessions
 * without requiring Clerk authentication. The token is generated
 * on session creation and must be passed in subsequent API requests.</p>
 *
 * <p>Security considerations:</p>
 * <ul>
 *   <li>Tokens use 256 bits of entropy from SecureRandom</li>
 *   <li>Tokens are Base64 URL-safe encoded for safe transmission</li>
 *   <li>Only the SHA-256 hash is stored in the database</li>
 *   <li>Original token is returned only once at creation time</li>
 * </ul>
 *
 * @author SkillSoft Development Team
 */
@Service
public class SessionTokenService {

    /**
     * Token length in bytes (256 bits = 32 bytes).
     * Results in 43 character Base64 URL-safe encoded string.
     */
    private static final int TOKEN_BYTES = 32;

    /**
     * Thread-safe secure random instance.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * SHA-256 algorithm name.
     */
    private static final String HASH_ALGORITHM = "SHA-256";

    /**
     * Generate a cryptographically secure random token.
     *
     * <p>The token is generated using SecureRandom with 256 bits of entropy
     * and encoded using Base64 URL-safe encoding without padding.</p>
     *
     * @return URL-safe Base64 encoded token (43 characters)
     */
    public String generateToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Hash a token using SHA-256.
     *
     * <p>Only the hash is stored in the database, not the original token.
     * This ensures that even if the database is compromised, the tokens
     * cannot be reconstructed.</p>
     *
     * @param token The token to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is required by the JVM spec, this should never happen
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Validate a token against a stored hash.
     *
     * <p>Computes the hash of the provided token and compares it
     * with the stored hash using constant-time comparison to
     * prevent timing attacks.</p>
     *
     * @param token      The token to validate
     * @param storedHash The stored hash to compare against
     * @return true if the token hash matches the stored hash
     */
    public boolean validateToken(String token, String storedHash) {
        if (token == null || storedHash == null) {
            return false;
        }
        String tokenHash = hashToken(token);
        return MessageDigest.isEqual(
                tokenHash.getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generate a token and its hash together.
     *
     * <p>Convenience method for session creation where both
     * the token (to return to client) and hash (to store) are needed.</p>
     *
     * @return TokenWithHash containing both the token and its hash
     */
    public TokenWithHash generateTokenWithHash() {
        String token = generateToken();
        String hash = hashToken(token);
        return new TokenWithHash(token, hash);
    }

    /**
     * Record containing a token and its hash.
     */
    public record TokenWithHash(
            /**
             * The plain token to return to the client.
             */
            String token,

            /**
             * The SHA-256 hash to store in the database.
             */
            String hash
    ) {
    }
}
