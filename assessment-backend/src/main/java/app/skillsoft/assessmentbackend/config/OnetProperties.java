package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for O*NET Web Services API integration.
 *
 * <p>When enabled, the O*NET service makes real HTTP calls to the
 * O*NET Web Services API. When disabled (default), mock data is used.</p>
 *
 * <p>Configuration:</p>
 * <pre>
 *   skillsoft.onet.enabled=false
 *   skillsoft.onet.base-url=https://services.onetcenter.org/ws/
 *   skillsoft.onet.username=${ONET_USERNAME:}
 *   skillsoft.onet.api-key=${ONET_API_KEY:}
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "skillsoft.onet")
public class OnetProperties {

    /**
     * Whether real O*NET API calls are enabled.
     * Default: false (uses mock data).
     */
    private boolean enabled = false;

    /**
     * Base URL for the O*NET Web Services API.
     */
    private String baseUrl = "https://services.onetcenter.org/ws/";

    /**
     * O*NET Web Services registered application name (used as HTTP Basic username).
     */
    private String username = "";

    /**
     * O*NET Web Services API key (used as HTTP Basic password).
     */
    private String apiKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
