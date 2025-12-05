package app.skillsoft.assessmentbackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.lang.NonNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration for allowing cross-origin requests from frontend.
 * 
 * This configuration ensures CORS headers are properly sent for all requests,
 * including OPTIONS preflight requests. The CorsFilter is registered with
 * highest precedence to ensure it runs before Spring Security filters.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(CorsConfig.class);

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        logger.info("CORS: Configuring WebMvcConfigurer CORS mappings");
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Creates the CORS configuration used by both Spring Security and the CorsFilter.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        logger.info("CORS: Configuring CorsConfigurationSource");
        
        // Allow all origins with patterns (required when allowCredentials=true)
        configuration.setAllowedOriginPatterns(List.of("*"));
        
        // Allow credentials for authentication
        configuration.setAllowCredentials(true);
        
        // Allow all headers including custom auth headers
        configuration.setAllowedHeaders(List.of(
            "*",
            "Origin",
            "Content-Type",
            "Accept",
            "Authorization",
            "X-User-Id",
            "X-User-Role",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Expose response headers to the client
        configuration.setExposedHeaders(List.of(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials",
            "Access-Control-Allow-Headers",
            "Access-Control-Allow-Methods",
            "Access-Control-Max-Age"
        ));
        
        // Allow all necessary HTTP methods
        configuration.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
        ));
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
    /**
     * Creates a CorsFilter bean with highest precedence.
     * This ensures CORS headers are added before any other filter processes the request,
     * which is critical for OPTIONS preflight requests.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        logger.info("CORS: Creating CorsFilter with highest precedence");
        return new CorsFilter(corsConfigurationSource());
    }
}