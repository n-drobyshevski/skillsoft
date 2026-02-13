package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Web configuration for response optimization.
 *
 * <p>Registers a {@link ShallowEtagHeaderFilter} that:
 * <ul>
 *   <li>Generates ETag headers based on response content hash</li>
 *   <li>Returns 304 Not Modified when client sends matching If-None-Match</li>
 *   <li>Reduces bandwidth for unchanged API responses</li>
 * </ul>
 *
 * <p>Combined with {@code server.compression.enabled=true}, this provides
 * both smaller payloads and conditional request support.
 */
@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> filterRegistration =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        filterRegistration.addUrlPatterns("/api/*");
        filterRegistration.setName("etagFilter");
        return filterRegistration;
    }
}
