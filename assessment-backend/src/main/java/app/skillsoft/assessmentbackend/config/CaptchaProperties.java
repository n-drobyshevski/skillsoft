package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for CAPTCHA verification on anonymous sessions.
 *
 * <p>When enabled, anonymous session creation requires a valid hCaptcha token.
 * Disabled by default to avoid breaking existing deployments.</p>
 *
 * <p>Configuration:</p>
 * <pre>
 *   skillsoft.captcha.enabled=false
 *   skillsoft.captcha.secret-key=${HCAPTCHA_SECRET_KEY:}
 *   skillsoft.captcha.site-key=${HCAPTCHA_SITE_KEY:}
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "skillsoft.captcha")
public class CaptchaProperties {

    /**
     * Whether CAPTCHA verification is enabled for anonymous session creation.
     * Default: false (feature-flagged off).
     */
    private boolean enabled = false;

    /**
     * hCaptcha secret key for server-side verification.
     */
    private String secretKey = "";

    /**
     * hCaptcha site key (exposed to frontend via config endpoint).
     */
    private String siteKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getSiteKey() {
        return siteKey;
    }

    public void setSiteKey(String siteKey) {
        this.siteKey = siteKey;
    }
}
