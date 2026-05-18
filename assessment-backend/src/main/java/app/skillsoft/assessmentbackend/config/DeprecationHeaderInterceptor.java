package app.skillsoft.assessmentbackend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that adds RFC 8594 Deprecation and Sunset headers to legacy V0 API paths.
 *
 * V0 paths are the original un-versioned endpoints (e.g., /api/competencies)
 * that have been superseded by /api/v1/* endpoints.
 *
 * Clients consuming these deprecated endpoints will receive:
 * - Deprecation: true
 * - Sunset: 2026-06-01
 * - Link: successor-version pointing to the /api/v1 equivalent
 */
@Component
public class DeprecationHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {
        String path = request.getRequestURI();
        if (isV0Path(path)) {
            response.setHeader("Deprecation", "true");
            response.setHeader("Sunset", "2026-06-01");
            response.setHeader("Link",
                    "</api/v1" + path.substring("/api".length()) + ">; rel=\"successor-version\"");
        }
        return true;
    }

    private boolean isV0Path(String path) {
        // Only paths whose legacy controllers still exist. The /api/competencies,
        // /api/behavioral-indicators, and /api/questions handlers were removed in
        // dev's "chore: remove AI artifacts and deprecated controllers" cleanup;
        // requests to them now 404 before reaching this interceptor's response.
        return path.startsWith("/api/users");
    }
}
