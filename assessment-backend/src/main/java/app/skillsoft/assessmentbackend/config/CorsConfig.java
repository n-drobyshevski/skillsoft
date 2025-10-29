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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/**
 * CORS configuration for allowing cross-origin requests from frontend
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

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
        logger.info("ALLOWED_ORIGINS environment variable: {}", allowedOrigins);
        
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            // Split by comma and trim each origin
            String[] originsArray = allowedOrigins.split(",");
            for (String origin : originsArray) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty()) {
                    origins.add(trimmedOrigin);
                }
            }
            logger.info("Using ALLOWED_ORIGINS: {}", origins);
            return origins;
        }
        
        // TEMPORARY: Allow all origins for testing (REMOVE IN PRODUCTION)
        String isDev = environment.getProperty("SPRING_PROFILES_ACTIVE", "");
        if (isDev.contains("dev") || isDev.contains("test")) {
            logger.warn("DEV MODE: Allowing all origins - THIS IS NOT SECURE FOR PRODUCTION!");
            origins.add("*");
            return origins;
        }
        
        // Fallback to SERVER_HOST-based origins for backward compatibility
        String host = environment.getProperty("SERVER_HOST", "localhost");
        logger.info("SERVER_HOST: {}, using fallback origins", host);
        origins.addAll(Arrays.asList(
            // Vercel deployment URLs - Replace with your actual Vercel URL
            "https://skillsoft-frontend.vercel.app",
            "https://frontend-app.vercel.app", 
            "https://skillsoft.vercel.app",
            "https://skillsoft-front.vercel.app",
            "https://skillsoft-git-main.vercel.app",
            "https://skillsoft-git-master.vercel.app",
            "https://skillsoft-git-dev.vercel.app",
            
            // Railway production URLs (no ports needed)
            "https://frontend-app-production-e74e.up.railway.app",
            "http://frontend-app-production-e74e.up.railway.app",
            
            // Custom domain URLs  
            "http://skillsoft.app",
            "https://skillsoft.app",
            
            // Railway internal networking
            "http://outstanding-presence.railway.internal",
            "https://outstanding-presence.railway.internal",
            
            // Local development
            "http://localhost:3000",
            "http://localhost:3001", 
            "http://localhost:3002",
            "http://localhost:3003",
            "http://localhost:5173",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "http://127.0.0.1:3002"
        ));
        
        logger.info("Final allowed origins: {}", origins);
        return origins;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        List<String> allowedOrigins = getAllowedOrigins();
        
        registry.addMapping("/**")  // Apply to all endpoints
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
        
        // Allow credentials for authentication
        configuration.setAllowCredentials(true);
        
        // Allow all headers including custom ones
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Expose headers that the client can access
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Methods",
            "Access-Control-Max-Age",
            "Access-Control-Request-Headers",
            "Access-Control-Request-Method"
        ));
        
        // Allow specific HTTP methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Configure how long the browser can cache preflight requests
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);  // Apply to all endpoints
        return source;
    }
}