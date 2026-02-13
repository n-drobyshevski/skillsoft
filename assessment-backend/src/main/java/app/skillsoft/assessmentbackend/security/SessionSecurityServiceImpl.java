package app.skillsoft.assessmentbackend.security;

import app.skillsoft.assessmentbackend.domain.entities.TestResult;
import app.skillsoft.assessmentbackend.domain.entities.TestSession;
import app.skillsoft.assessmentbackend.repository.TestResultRepository;
import app.skillsoft.assessmentbackend.repository.TestSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of session ownership verification for resource-level authorization.
 *
 * This service is used by @PreAuthorize SpEL expressions:
 * - @PreAuthorize("@sessionSecurity.isSessionOwner(#sessionId)")
 * - @PreAuthorize("@sessionSecurity.isResultOwner(#resultId)")
 * - @PreAuthorize("@sessionSecurity.canAccessUserData(#clerkUserId)")
 */
@Service("sessionSecurity")
public class SessionSecurityServiceImpl implements SessionSecurityService {

    private static final Logger log = LoggerFactory.getLogger(SessionSecurityServiceImpl.class);

    private final TestSessionRepository sessionRepository;
    private final TestResultRepository resultRepository;

    public SessionSecurityServiceImpl(
            TestSessionRepository sessionRepository,
            TestResultRepository resultRepository) {
        this.sessionRepository = sessionRepository;
        this.resultRepository = resultRepository;
    }

    @Override
    public boolean isSessionOwner(UUID sessionId) {
        if (sessionId == null) {
            log.warn("isSessionOwner called with null sessionId");
            return false;
        }

        String authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId == null) {
            log.warn("No authenticated user for session ownership check");
            return false;
        }

        // Admins and Editors can access any session
        if (hasPrivilegedRole()) {
            log.debug("User {} has privileged access to session {}", authenticatedUserId, sessionId);
            return true;
        }

        Optional<TestSession> session = sessionRepository.findById(sessionId);
        if (session.isEmpty()) {
            log.debug("Session {} not found", sessionId);
            return false;
        }

        boolean isOwner = authenticatedUserId.equals(session.get().getClerkUserId());
        if (!isOwner) {
            log.warn("Access denied: user {} attempted to access session {} owned by {}",
                    authenticatedUserId, sessionId, session.get().getClerkUserId());
        }
        return isOwner;
    }

    @Override
    public boolean isResultOwner(UUID resultId) {
        if (resultId == null) {
            log.warn("isResultOwner called with null resultId");
            return false;
        }

        String authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId == null) {
            log.warn("No authenticated user for result ownership check");
            return false;
        }

        // Admins and Editors can access any result
        if (hasPrivilegedRole()) {
            log.debug("User {} has privileged access to result {}", authenticatedUserId, resultId);
            return true;
        }

        Optional<TestResult> result = resultRepository.findById(resultId);
        if (result.isEmpty()) {
            log.debug("Result {} not found", resultId);
            return false;
        }

        boolean isOwner = authenticatedUserId.equals(result.get().getClerkUserId());
        if (!isOwner) {
            log.warn("Access denied: user {} attempted to access result {} owned by {}",
                    authenticatedUserId, resultId, result.get().getClerkUserId());
        }
        return isOwner;
    }

    @Override
    public boolean canAccessUserData(String targetClerkUserId) {
        if (targetClerkUserId == null || targetClerkUserId.isBlank()) {
            log.warn("canAccessUserData called with null/blank targetClerkUserId");
            return false;
        }

        String authenticatedUserId = getAuthenticatedUserId();
        if (authenticatedUserId == null) {
            log.warn("No authenticated user for user data access check");
            return false;
        }

        // Users can always access their own data
        if (authenticatedUserId.equals(targetClerkUserId)) {
            return true;
        }

        // Admins and Editors can access any user's data
        if (hasPrivilegedRole()) {
            log.debug("User {} has privileged access to user data for {}",
                    authenticatedUserId, targetClerkUserId);
            return true;
        }

        log.warn("Access denied: user {} attempted to access data for user {}",
                authenticatedUserId, targetClerkUserId);
        return false;
    }

    @Override
    public String getAuthenticatedUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof String userId) {
            return userId;
        }

        // Handle case where principal might be a different type
        return principal != null ? principal.toString() : null;
    }

    /**
     * Check if the authenticated user has ADMIN or EDITOR role.
     */
    private boolean hasPrivilegedRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(auth -> "ROLE_ADMIN".equals(auth) || "ROLE_EDITOR".equals(auth));
    }
}
