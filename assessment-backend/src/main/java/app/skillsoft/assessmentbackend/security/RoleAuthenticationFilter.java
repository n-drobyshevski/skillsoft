package app.skillsoft.assessmentbackend.security;

import app.skillsoft.assessmentbackend.domain.entities.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom authentication filter that extracts user information from request headers.
 * 
 * This filter works with the Next.js frontend which handles Clerk.js authentication
 * and passes user information to the backend via headers:
 * 
 * Headers:
 * - X-User-Id: The Clerk user ID
 * - X-User-Role: The user's role (ADMIN, EDITOR, USER)
 * 
 * The filter creates a Spring Security Authentication object with:
 * - Principal: The user ID (Clerk ID)
 * - Credentials: null (already authenticated by Clerk)
 * - Authorities: Role-based authorities (ROLE_ADMIN, ROLE_EDITOR, ROLE_USER)
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

    // Header names for user information
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

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

        // Parse the user role, default to USER if invalid or not provided
        UserRole userRole = parseUserRole(userRoleHeader);
        
        // Create authorities based on role hierarchy
        List<GrantedAuthority> authorities = createAuthorities(userRole);

        // Create authentication token
        UsernamePasswordAuthenticationToken authentication = 
            new UsernamePasswordAuthenticationToken(userId, null, authorities);
        
        // Set additional details
        authentication.setDetails(new UserAuthenticationDetails(userId, userRole));

        // Set the authentication in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        logger.debug("Authenticated user: {} with role: {} and authorities: {}", 
            userId, userRole, authorities);

        filterChain.doFilter(request, response);
    }

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
     * Custom authentication details class to hold additional user information.
     */
    public record UserAuthenticationDetails(String clerkId, UserRole role) {}
}
