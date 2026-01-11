package app.skillsoft.assessmentbackend.config;

import app.skillsoft.assessmentbackend.security.RoleAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Security configuration for the Skillsoft Assessment Backend.
 * 
 * This configuration:
 * - Enables method-level security with @PreAuthorize annotations
 * - Uses a custom filter to extract user roles from request headers
 * - Works with Clerk.js authentication in the frontend
 * - Maintains stateless session management (no server-side sessions)
 * 
 * Role-based access control is enforced at two levels:
 * 1. Frontend (Next.js middleware) - Route-level protection
 * 2. Backend (@PreAuthorize annotations) - Method-level protection
 * 
 * Note: CORS is handled by CorsFilter with highest precedence.
 * This ensures preflight OPTIONS requests receive proper headers
 * before Spring Security processes them.
 * 
 * Note: This configuration is excluded during tests (profile != test).
 * Tests use TestSecurityConfig instead.
 * 
 * @see RoleAuthenticationFilter
 * @see CorsConfig
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Profile("!test")
@Order(2) // After CorsFilter which has HIGHEST_PRECEDENCE
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final RoleAuthenticationFilter roleAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(RoleAuthenticationFilter roleAuthenticationFilter, 
                          CorsConfigurationSource corsConfigurationSource) {
        this.roleAuthenticationFilter = roleAuthenticationFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    /**
     * Configure the security filter chain.
     * 
     * Security rules:
     * - Clerk webhooks are public (no auth required)
     * - Health endpoints are public
     * - OPTIONS requests are allowed for CORS preflight
     * - All other requests require authentication via role header
     * 
     * Note: CORS is primarily handled by CorsFilter bean with highest precedence.
     * The cors() configuration here provides backup/integration with Spring Security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("Configuring Spring Security filter chain");

        http
            // Disable CSRF - we're using stateless API with header-based auth
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure CORS - uses the same CorsConfigurationSource as CorsFilter
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // Stateless session management
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // Configure request authorization
            .authorizeHttpRequests(authz -> authz
                // Public endpoints - no authentication required
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Allow CORS preflight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                
                // Allow public read access (GET) for content endpoints (legacy v0)
                .requestMatchers(HttpMethod.GET, "/api/competencies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/behavioral-indicators/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/questions/**").permitAll()

                // Allow public read access (GET) for content endpoints (v1)
                .requestMatchers(HttpMethod.GET, "/api/v1/competencies/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/behavioral-indicators/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/questions/**").permitAll()

                // Allow public access for share link validation (anonymous access support)
                .requestMatchers(HttpMethod.GET, "/api/v1/tests/templates/validate-link").permitAll()

                // Allow public access for anonymous test-taking via share links
                // These endpoints use session access tokens instead of Clerk JWT
                .requestMatchers("/api/v1/anonymous/**").permitAll()

                // All other requests require authentication
                // Role-based access is handled by @PreAuthorize annotations
                .anyRequest().authenticated()
            )
            
            // Add our custom role authentication filter
            .addFilterBefore(roleAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        logger.info("Security filter chain configured successfully");
        return http.build();
    }
}
