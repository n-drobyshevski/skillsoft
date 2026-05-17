package app.skillsoft.assessmentbackend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;

/**
 * Service for generating and validating HMAC-signed result view tokens.
 *
 * <p>Generates stateless, time-limited tokens that allow anonymous takers
 * to access their test results via a shareable URL without authentication.
 * The token encodes the result ID, session ID, and an expiry timestamp,
 * signed with HMAC-SHA256 using the application's shared secret.</p>
 *
 * <p>Token format: Base64URL(resultId:sessionId:expiryEpochSeconds:hmacSignature)</p>
 *
 * <p>Security properties:</p>
 * <ul>
 *   <li>HMAC-SHA256 prevents forgery — only the server can generate valid tokens</li>
 *   <li>Time-limited — tokens expire after a configurable duration (default 7 days)</li>
 *   <li>Constant-time signature comparison prevents timing attacks</li>
 *   <li>No database storage needed — fully stateless validation</li>
 * </ul>
 *
 * @author SkillSoft Development Team
 */
@Service
public class ResultTokenService {

    private static final Logger log = LoggerFactory.getLogger(ResultTokenService.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Default token validity: 7 days in seconds.
     */
    private static final long DEFAULT_EXPIRY_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Fallback secret used only in development when no HMAC secret is configured.
     */
    private static final String DEV_FALLBACK_SECRET = "skillsoft-dev-result-token-secret";

    private final String hmacSecret;
    private final long expirySeconds;

    public ResultTokenService(
            @Value("${app.security.hmac-secret:}") String hmacSecret,
            @Value("${app.security.result-token-expiry-seconds:604800}") long expirySeconds) {
        if (hmacSecret == null || hmacSecret.isBlank()) {
            log.warn("No HMAC secret configured — using development fallback for result tokens. "
                    + "Set HMAC_SHARED_SECRET in production.");
            this.hmacSecret = DEV_FALLBACK_SECRET;
        } else {
            this.hmacSecret = hmacSecret;
        }
        this.expirySeconds = expirySeconds > 0 ? expirySeconds : DEFAULT_EXPIRY_SECONDS;
    }

    /**
     * Generate a signed result view token.
     *
     * @param resultId  The result UUID
     * @param sessionId The session UUID
     * @return URL-safe Base64-encoded token string
     */
    public String generateToken(UUID resultId, UUID sessionId) {
        long expiryEpoch = Instant.now().getEpochSecond() + expirySeconds;
        String payload = resultId + ":" + sessionId + ":" + expiryEpoch;
        String signature = computeHmac(payload);
        String token = payload + ":" + signature;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Validate a result view token and extract the result ID.
     *
     * @param token The Base64URL-encoded token
     * @return The result UUID if valid, null if invalid or expired
     */
    public UUID validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(
                    Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = decoded.split(":", 4);

            if (parts.length != 4) {
                log.debug("Invalid result token format — expected 4 parts, got {}", parts.length);
                return null;
            }

            String resultIdStr = parts[0];
            String sessionIdStr = parts[1];
            String expiryStr = parts[2];
            String providedSignature = parts[3];

            // Verify expiry
            long expiryEpoch = Long.parseLong(expiryStr);
            if (Instant.now().getEpochSecond() > expiryEpoch) {
                log.debug("Result token expired for result {}", resultIdStr);
                return null;
            }

            // Verify HMAC signature (constant-time comparison)
            String payload = resultIdStr + ":" + sessionIdStr + ":" + expiryStr;
            String expectedSignature = computeHmac(payload);

            if (!MessageDigest.isEqual(
                    providedSignature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                log.debug("Invalid result token signature for result {}", resultIdStr);
                return null;
            }

            return UUID.fromString(resultIdStr);
        } catch (IllegalArgumentException e) {
            log.debug("Failed to parse result token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Compute HMAC-SHA256 of the given message.
     */
    private String computeHmac(String message) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(rawHmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC for result token", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
