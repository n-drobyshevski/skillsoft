package app.skillsoft.assessmentbackend.services;

import app.skillsoft.assessmentbackend.config.CaptchaProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Service for verifying hCaptcha tokens on anonymous session creation.
 *
 * <p>When CAPTCHA is enabled via {@link CaptchaProperties}, this service
 * validates the client-provided hCaptcha response token against the
 * hCaptcha siteverify API.</p>
 *
 * <p>When CAPTCHA is disabled, all verification calls return true (no-op).</p>
 */
@Service
public class CaptchaVerificationService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaVerificationService.class);
    private static final String HCAPTCHA_VERIFY_URL = "https://api.hcaptcha.com/siteverify";

    private final CaptchaProperties captchaProperties;
    private final RestClient restClient;

    public CaptchaVerificationService(CaptchaProperties captchaProperties) {
        this.captchaProperties = captchaProperties;
        this.restClient = RestClient.create();
    }

    /**
     * Check whether CAPTCHA verification is enabled.
     *
     * @return true if CAPTCHA is required for anonymous sessions
     */
    public boolean isEnabled() {
        return captchaProperties.isEnabled();
    }

    /**
     * Get the hCaptcha site key for client-side rendering.
     *
     * @return the site key, or empty string if not configured
     */
    public String getSiteKey() {
        return captchaProperties.getSiteKey();
    }

    /**
     * Verify an hCaptcha token.
     *
     * <p>When CAPTCHA is disabled, always returns true.
     * When enabled, validates the token via the hCaptcha API.</p>
     *
     * @param captchaToken the hCaptcha response token from the client
     * @return true if the token is valid (or CAPTCHA is disabled)
     */
    public boolean verify(String captchaToken) {
        if (!captchaProperties.isEnabled()) {
            return true;
        }

        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("CAPTCHA verification failed: no token provided");
            return false;
        }

        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", captchaProperties.getSecretKey());
            formData.add("response", captchaToken);

            HCaptchaResponse response = restClient.post()
                    .uri(HCAPTCHA_VERIFY_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(HCaptchaResponse.class);

            if (response == null) {
                log.error("CAPTCHA verification failed: null response from hCaptcha API");
                return false;
            }

            if (!response.success()) {
                log.warn("CAPTCHA verification failed: errorCodes={}", response.errorCodes());
            }

            return response.success();
        } catch (Exception e) {
            log.error("CAPTCHA verification failed with exception: {}", e.getMessage(), e);
            // Fail open on API errors to avoid blocking legitimate users
            // when hCaptcha is temporarily unavailable
            return true;
        }
    }

    /**
     * Response from hCaptcha siteverify API.
     */
    private record HCaptchaResponse(
            boolean success,
            @JsonProperty("error-codes")
            String[] errorCodes
    ) {}
}
