package app.skillsoft.assessmentbackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * CORS configuration for allowing cross-origin requests from frontend
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Autowired
    private Environment environment;

    /**
     * Parse allowed origins from environment variables
     * Supports both ALLOWED_ORIGINS (comma-separated) and SERVER_HOST (legacy)
     */
    private List<String> getAllowedOrigins() {
        List<String> origins = new ArrayList<>();
        
        // Check for ALLOWED_ORIGINS environment variable first
        String allowedOrigins = environment.getProperty("ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            // Split by comma and trim each origin
            String[] originsArray = allowedOrigins.split(",");
            for (String origin : originsArray) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty()) {
                    origins.add(trimmedOrigin);
                }
            }
            return origins;
        }
        
        // Fallback to SERVER_HOST-based origins for backward compatibility
        String host = environment.getProperty("env.server.host", "localhost");
        origins.addAll(Arrays.asList(
            "http://outstanding-presence.railway.internal" +  ":3000",
            "http://outstanding-presence.railway.internal" +  ":3001",
            "http://outstanding-presence.railway.internal" + ":3002",
                "http://outstanding-presence.railway.internal" + ":3003",
            "http://outstanding-presence.railway.internal" + ":5173",
                "http://" + "localhost" + ":3000",
                "http://" + "localhost" + ":3001",
                "http://" + "localhost" + ":3002",
                "http://" + "localhost" + ":3003",
                "http://" + "localhost" + ":5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://127.0.0.1:3002",
                "http://127.0.0.1:3002"
        ));
        
        return origins;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        List<String> allowedOrigins = getAllowedOrigins();
        
        registry.addMapping("/**")  // Changed from /api/** to /** to cover all endpoints
                .allowedOrigins(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        List<String> allowedOrigins = getAllowedOrigins();

        // Allow specific origins (frontend URLs)
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Allow all headers
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Allow credentials (cookies, authorization headers)
        configuration.setAllowCredentials(true);
        
        // Configure how long the browser can cache preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // Changed from /api/** to /**
        return source;
    }
}