package app.skillsoft.assessmentbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Competency Passport subsystem.
 *
 * <p>Configuration:</p>
 * <pre>
 *   skillsoft.passport.validity-days=180
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "skillsoft.passport")
public class PassportProperties {

    /**
     * Number of days a passport remains valid after the last assessment.
     * Default: 180 days (~6 months).
     */
    private int validityDays = 180;

    public int getValidityDays() {
        return validityDays;
    }

    public void setValidityDays(int validityDays) {
        this.validityDays = validityDays;
    }
}
