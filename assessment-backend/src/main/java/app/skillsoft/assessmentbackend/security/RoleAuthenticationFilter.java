package app.skillsoft.assessmentbackend.security;

import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom authentication filter that extracts user information from request headers.
 *
 * This filter works with the Next.js frontend which handles Clerk.js authentication
 * and passes user information to the backend via headers:
 *
 * Headers:
 * - X-User-Id: The Clerk user ID
 * - X-User-Role: The user's role (ADMIN, EDITOR, USER)
 * - X-Auth-Timestamp: Unix epoch seconds when the request was signed
 * - X-Auth-Signature: HMAC-SHA256 hex signature of "userId:userRole:timestamp"
 *
 * The filter creates a Spring Security Authentication object with:
 * - Principal: The user ID (Clerk ID)
 * - Credentials: null (already authenticated by Clerk)
 * - Authorities: Role-based authorities (ROLE_ADMIN, ROLE_EDITOR, ROLE_USER)
 *
 * HMAC Verification:
 * - When app.security.hmac-secret is configured, the filter verifies X-Auth-Signature
 *   to prevent header spoofing. Requests with invalid/expired signatures are rejected with 401.
 * - When the secret is NOT configured (empty), the filter falls back to trust-based behavior
 *   for backward compatibility in development environments.
 *
 * This allows @PreAuthorize annotations to work properly:
 * - @PreAuthorize("hasRole('ADMIN')") - Admin only
 * - @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')") - Admin or Editor
 * - @PreAuthorize("hasRole('USER')") - All authenticated users
 *
 * Note: This filter is excluded during tests (profile != test).
 */
@Component
@Profile("!test")
public class RoleAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RoleAuthenticationFilter.class);

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /** Maximum allowed age of a signed request, in seconds (5 minutes). */
    private static final long MAX_TIMESTAMP_DRIFT_SECONDS = 300;

    // Header names for user information
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String EFFECTIVE_ROLE_HEADER = "X-Effective-Role";
    public static final String AUTH_TIMESTAMP_HEADER = "X-Auth-Timestamp";
    public static final String AUTH_SIGNATURE_HEADER = "X-Auth-Signature";

    /** Role hierarchy levels for downgrade validation. */
    private static final Map<UserRole, Integer> ROLE_LEVEL = Map.of(
            UserRole.USER, 0, UserRole.EDITOR, 1, UserRole.ADMIN, 2);

    @Value("${app.security.hmac-secret:}")
    private String hmacSecret;

    /** Whether HMAC verification is active (secret is configured and non-empty). */
    private boolean hmacEnabled;

    @PostConstruct
    void init() {
        hmacEnabled = hmacSecret != null && !hmacSecret.isBlank();
        if (hmacEnabled) {
            logger.info("HMAC header verification is ENABLED. "
                    + "All authenticated requests must include valid X-Auth-Signature.");
        } else {
            logger.warn("HMAC header verification is DISABLED. "
                    + "Running in trust-based header mode. "
                    + "Set HMAC_SHARED_SECRET environment variable to enable signature verification.");
        }
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userRoleHeader = request.getHeader(USER_ROLE_HEADER);
        String path = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("Processing request: {} {} - userId: {}, role: {}", method, path, userId, userRoleHeader);

        // Skip authentication for public endpoints
        if (isPublicEndpoint(path, method)) {
            logger.debug("Skipping authentication for public endpoint: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        // If no user ID provided, continue without authentication
        // Spring Security will handle unauthorized access
        if (userId == null || userId.isBlank()) {
            logger.debug("No user ID provided for protected endpoint: {} {}", method, path);
            filterChain.doFilter(request, response);
            return;
        }

        // Read effective role header (lens-based permission downgrade)
        String effectiveRoleHeader = request.getHeader(EFFECTIVE_ROLE_HEADER);

        // --- HMAC Verification ---
        if (hmacEnabled) {
            String timestamp = request.getHeader(AUTH_TIMESTAMP_HEADER);
            String signature = request.getHeader(AUTH_SIGNATURE_HEADER);

            if (!verifyHmacSignature(userId, userRoleHeader, effectiveRoleHeader, timestamp, signature, response)) {
                // Response already committed with 401 inside verifyHmacSignature
                return;
            }
        }

        // Parse the actual user role, default to USER if invalid or not provided
        UserRole actualRole = parseUserRole(userRoleHeader);

        // Resolve effective role (lens downgrade), validated against actual role
        UserRole effectiveRole = resolveEffectiveRole(actualRole, effectiveRoleHeader);

        if (effectiveRole != actualRole) {
            logger.info("Lens downgrade active: user={}, actual={}, effective={}",
                    userId, actualRole, effectiveRole);
        }

        // Create authorities based on EFFECTIVE role (not actual) for permission enforcement
        List<GrantedAuthority> authorities = createAuthorities(effectiveRole);

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // Set details with both roles for audit trail
        authentication.setDetails(new UserAuthenticationDetails(userId, actualRole, effectiveRole));

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.debug("Authenticated user: {} with actual={}, effective={}, authorities: {}",
            userId, actualRole, effectiveRole, authorities);

        filterChain.doFilter(request, response);
    }

    // ========================================================================
    // HMAC Verification
    // ========================================================================

    /**
     * Verify the HMAC-SHA256 signature of the authentication headers.
     *
     * Supports two message formats for backward compatibility:
     * - With effective role: "userId:userRole:effectiveRole:timestamp"
     * - Without effective role: "userId:userRole:timestamp"
     *
     * @return {@code true} if the signature is valid; {@code false} if the request
     *         was rejected (401 already written to response).
     */
    private boolean verifyHmacSignature(
            String userId,
            String userRole,
            String effectiveRole,
            String timestamp,
            String signature,
            HttpServletResponse response) throws IOException {

        // Check that both HMAC headers are present
        if (timestamp == null || timestamp.isBlank() || signature == null || signature.isBlank()) {
            logger.warn("HMAC verification failed: missing X-Auth-Timestamp or X-Auth-Signature header");
            rejectRequest(response, "Missing authentication signature headers");
            return false;
        }

        // Validate timestamp format and freshness
        long requestEpoch;
        try {
            requestEpoch = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            logger.warn("HMAC verification failed: invalid timestamp format '{}'", timestamp);
            rejectRequest(response, "Invalid authentication timestamp");
            return false;
        }

        long nowEpoch = System.currentTimeMillis() / 1000;
        long drift = Math.abs(nowEpoch - requestEpoch);
        if (drift > MAX_TIMESTAMP_DRIFT_SECONDS) {
            logger.warn("HMAC verification failed: timestamp drift {}s exceeds maximum {}s "
                    + "(request={}, server={})",
                    drift, MAX_TIMESTAMP_DRIFT_SECONDS, requestEpoch, nowEpoch);
            rejectRequest(response, "Authentication timestamp expired");
            return false;
        }

        // Compute expected signature
        // With effective role: "userId:userRole:effectiveRole:timestamp"
        // Without effective role: "userId:userRole:timestamp"
        String rolesPart = (userRole != null ? userRole : "");
        String message;
        if (effectiveRole != null && !effectiveRole.isBlank()) {
            message = userId + ":" + rolesPart + ":" + effectiveRole + ":" + timestamp;
        } else {
            message = userId + ":" + rolesPart + ":" + timestamp;
        }
        String expectedSignature;
        try {
            expectedSignature = computeHmacHex(message);
        } catch (Exception e) {
            logger.error("HMAC computation error", e);
            rejectRequest(response, "Internal authentication error");
            return false;
        }

        // Constant-time comparison to prevent timing attacks
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8))) {
            logger.warn("HMAC verification failed: signature mismatch for user '{}'", userId);
            rejectRequest(response, "Invalid authentication signature");
            return false;
        }

        logger.debug("HMAC verification passed for user '{}'", userId);
        return true;
    }

    /**
     * Compute HMAC-SHA256 of the given message using the configured secret and return as hex string.
     */
    private String computeHmacHex(String message) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec keySpec = new SecretKeySpec(
                hmacSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        mac.init(keySpec);
        byte[] rawHmac = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(rawHmac);
    }

    /**
     * Convert a byte array to a lowercase hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Write a 401 Unauthorized JSON response.
     */
    private void rejectRequest(HttpServletResponse response, String reason) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":401,\"message\":\"" + reason + "\",\"code\":\"HMAC_VERIFICATION_FAILED\"}");
        response.getWriter().flush();
    }

    // ========================================================================
    // Existing helper methods (unchanged)
    // ========================================================================

    /**
     * Check if the endpoint is public and doesn't require authentication.
     */
    private boolean isPublicEndpoint(String path, String method) {
        // CORS preflight requests
        if ("OPTIONS".equals(method)) {
            return true;
        }

        // Public endpoints
        return path.startsWith("/api/webhooks/") ||
               path.startsWith("/actuator/health") ||
               path.equals("/error");
    }

    /**
     * Parse the user role from the header value.
     * Defaults to USER if the role is invalid or not provided.
     */
    private UserRole parseUserRole(String roleHeader) {
        if (roleHeader == null || roleHeader.isBlank()) {
            return UserRole.USER;
        }

        try {
            return UserRole.valueOf(roleHeader.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role '{}', defaulting to USER", roleHeader);
            return UserRole.USER;
        }
    }

    /**
     * Create Spring Security authorities based on the user's role.
     *
     * Role hierarchy:
     * - ADMIN: Has ROLE_ADMIN, ROLE_EDITOR, ROLE_USER
     * - EDITOR: Has ROLE_EDITOR, ROLE_USER
     * - USER: Has ROLE_USER only
     *
     * This allows hasRole('ADMIN') to work as expected.
     */
    private List<GrantedAuthority> createAuthorities(UserRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        switch (role) {
            case ADMIN:
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                authorities.add(new SimpleGrantedAuthority("ROLE_EDITOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            case EDITOR:
                authorities.add(new SimpleGrantedAuthority("ROLE_EDITOR"));
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
            case USER:
            default:
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                break;
        }

        return authorities;
    }

    /**
     * Resolve the effective role from the X-Effective-Role header.
     * The effective role can only be equal to or lower than the actual role (downgrade only).
     * If the header is absent or invalid, the actual role is returned unchanged.
     *
     * @param actualRole     the user's real role from Clerk
     * @param effectiveHeader the X-Effective-Role header value (may be null)
     * @return the resolved effective role, guaranteed <= actualRole
     */
    private UserRole resolveEffectiveRole(UserRole actualRole, String effectiveHeader) {
        if (effectiveHeader == null || effectiveHeader.isBlank()) {
            return actualRole;
        }

        UserRole requested = parseUserRole(effectiveHeader);

        // Only allow downgrade, never escalation
        if (ROLE_LEVEL.getOrDefault(requested, 0) <= ROLE_LEVEL.getOrDefault(actualRole, 0)) {
            return requested;
        }

        logger.warn("Privilege escalation attempt blocked: actual={}, requested={}", actualRole, requested);
        return actualRole;
    }

    /**
     * Authentication details carrying both the actual Clerk role and the effective
     * (lens-downgraded) role for audit trail purposes.
     */
    public record UserAuthenticationDetails(String clerkId, UserRole actualRole, UserRole effectiveRole) {}
}
