package app.skillsoft.assessmentbackend.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * DTO for standard occupational and skill taxonomy mappings.
 * <p>
 * Provides type-safe, validated container for external standard references
 * including O*NET occupational codes, ESCO skill/competence URIs, and Big Five personality traits.
 * </p>
 *
 * <h3>Supported Standards:</h3>
 * <ul>
 *   <li><strong>O*NET</strong>: US Department of Labor's Occupational Information Network</li>
 *   <li><strong>ESCO</strong>: European Skills, Competences, Qualifications and Occupations</li>
 *   <li><strong>Big Five</strong>: Personality trait classification (OCEAN model)</li>
 * </ul>
 *
 * <h3>Example JSON Structure:</h3>
 * <pre>{@code
 * {
 *   "bigFiveRef": {
 *     "trait": "CONSCIENTIOUSNESS",
 *     "title": "Conscientiousness",
 *     "facet": "achievement_striving"
 *   },
 *   "onetRef": {
 *     "code": "2.B.1.a",
 *     "title": "Oral Comprehension",
 *     "elementType": "ability"
 *   },
 *   "escoRef": {
 *     "uri": "http://data.europa.eu/esco/skill/abc123",
 *     "title": "Communication Skills",
 *     "skillType": "skill"
 *   }
 * }
 * }</pre>
 *
 * <h3>Jackson Configuration Notes:</h3>
 * <p>
 * This class uses simple camelCase field names that match Java record conventions.
 * All nested DTOs (BigFiveRefDto, OnetRefDto, EscoRefDto) follow the same pattern
 * with simple field names like code, uri, trait, title, etc.
 * </p>
 *
 * @see OnetRefDto
 * @see EscoRefDto
 * @see BigFiveRefDto
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StandardCodesDto(
        @Valid
        @JsonAlias({"globalCategory", "global_category"})
        BigFiveRefDto bigFiveRef,

        @Valid
        @JsonAlias("onet_ref")
        OnetRefDto onetRef,

        @Valid
        @JsonAlias("esco_ref")
        EscoRefDto escoRef
) implements Serializable {

    /**
     * Creates an empty StandardCodesDto with all null references.
     */
    public StandardCodesDto() {
        this(null, null, null);
    }

    /**
     * Checks if this DTO has any standard code mappings defined.
     *
     * @return true if at least one mapping is present
     */
    public boolean hasAnyMapping() {
        return bigFiveRef != null || onetRef != null || escoRef != null;
    }

    /**
     * Checks if this DTO has a complete O*NET reference.
     *
     * @return true if O*NET reference is present with valid code
     */
    public boolean hasOnetMapping() {
        return onetRef != null && onetRef.code() != null && !onetRef.code().isBlank();
    }

    /**
     * Checks if this DTO has a complete ESCO reference.
     *
     * @return true if ESCO reference is present with valid URI
     */
    public boolean hasEscoMapping() {
        return escoRef != null && escoRef.uri() != null && !escoRef.uri().isBlank();
    }

    /**
     * Checks if this DTO has a complete Big Five reference.
     *
     * @return true if Big Five reference is present with valid trait
     */
    public boolean hasBigFiveMapping() {
        return bigFiveRef != null && bigFiveRef.trait() != null && !bigFiveRef.trait().isBlank();
    }

    /**
     * Big Five (OCEAN Model) personality trait reference DTO.
     * <p>
     * The Big Five personality traits are:
     * </p>
     * <ul>
     *   <li>OPENNESS: Creativity, curiosity, openness to new experiences</li>
     *   <li>CONSCIENTIOUSNESS: Organization, dependability, self-discipline</li>
     *   <li>EXTRAVERSION: Sociability, assertiveness, positive emotions</li>
     *   <li>AGREEABLENESS: Cooperation, trust, concern for others</li>
     *   <li>EMOTIONAL_STABILITY: Calmness, resilience, stress tolerance</li>
     * </ul>
     *
     * @param trait Big Five dimension code (e.g., "CONSCIENTIOUSNESS")
     * @param title Human-readable display name (e.g., "Conscientiousness")
     * @param facet Optional sub-facet of the trait (e.g., "achievement_striving", "self_discipline")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BigFiveRefDto(
            @JsonAlias("bigFive")
            @NotBlank(message = "Big Five trait is required")
            @Pattern(
                    regexp = "^(OPENNESS|CONSCIENTIOUSNESS|EXTRAVERSION|AGREEABLENESS|EMOTIONAL_STABILITY)$",
                    message = "Trait must be one of: OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, EMOTIONAL_STABILITY"
            )
            String trait,

            @Size(max = 100, message = "Title must not exceed 100 characters")
            String title,

            @JsonAlias("dimension")
            @Size(max = 100, message = "Facet must not exceed 100 characters")
            String facet
    ) implements Serializable {

        /**
         * Creates a BigFiveRefDto with only the trait.
         *
         * @param trait Big Five dimension code
         */
        public BigFiveRefDto(String trait) {
            this(trait, null, null);
        }

        /**
         * Creates a BigFiveRefDto with trait and title.
         *
         * @param trait Big Five dimension code
         * @param title Human-readable display name
         */
        public BigFiveRefDto(String trait, String title) {
            this(trait, title, null);
        }
    }

    /**
     * O*NET (Occupational Information Network) reference DTO.
     * <p>
     * O*NET codes follow specific patterns depending on element type:
     * </p>
     * <ul>
     *   <li>Abilities: X.X.X.X (e.g., "1.A.1.a")</li>
     *   <li>Skills: X.X.X (e.g., "2.B.1")</li>
     *   <li>Knowledge: X.X.X (e.g., "2.C.1")</li>
     *   <li>Work Activities: X.X.X.X (e.g., "4.A.1.a")</li>
     * </ul>
     *
     * @param code        O*NET element code (e.g., "2.B.1.a")
     * @param title       Human-readable title from O*NET database
     * @param elementType Type of O*NET element: ability, skill, knowledge, work_activity, work_style
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record OnetRefDto(
            @NotBlank(message = "O*NET code is required")
            @Pattern(
                    regexp = "^\\d+\\.[A-Z](\\.\\d+)?(\\.\\w+)?$",
                    message = "O*NET code must follow pattern like '2.B.1.a' or '1.A.1'"
            )
            @Size(max = 20, message = "O*NET code must not exceed 20 characters")
            String code,

            @Size(max = 255, message = "O*NET title must not exceed 255 characters")
            String title,

            @JsonAlias("element_type")
            @Pattern(
                    regexp = "^(ability|skill|knowledge|work_activity|work_style|interest|work_value|work_context)$",
                    message = "Element type must be one of: ability, skill, knowledge, work_activity, work_style, interest, work_value, work_context"
            )
            String elementType
    ) implements Serializable {

        /**
         * Creates an OnetRefDto with only the code.
         *
         * @param code O*NET element code
         */
        public OnetRefDto(String code) {
            this(code, null, null);
        }

        /**
         * Creates an OnetRefDto with code and title.
         *
         * @param code  O*NET element code
         * @param title Human-readable title
         */
        public OnetRefDto(String code, String title) {
            this(code, title, null);
        }
    }

    /**
     * ESCO (European Skills, Competences, Qualifications and Occupations) reference DTO.
     * <p>
     * ESCO URIs are persistent identifiers from the European Commission's ESCO classification.
     * </p>
     *
     * @param uri       ESCO persistent URI (e.g., "http://data.europa.eu/esco/skill/...")
     * @param title     Human-readable label from ESCO
     * @param skillType Type classification: skill, competence, knowledge, language, transversal
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EscoRefDto(
            @NotBlank(message = "ESCO URI is required")
            @Pattern(
                    regexp = "^https?://data\\.europa\\.eu/esco/(skill|occupation|isced-f|qualification)/[a-fA-F0-9-]+$",
                    message = "ESCO URI must be a valid data.europa.eu/esco resource identifier"
            )
            @Size(max = 500, message = "ESCO URI must not exceed 500 characters")
            String uri,

            @Size(max = 255, message = "ESCO title must not exceed 255 characters")
            String title,

            @JsonAlias("skill_type")
            @Pattern(
                    regexp = "^(skill|competence|knowledge|language|transversal)$",
                    message = "Skill type must be one of: skill, competence, knowledge, language, transversal"
            )
            String skillType
    ) implements Serializable {

        /**
         * Creates an EscoRefDto with only the URI.
         *
         * @param uri ESCO persistent URI
         */
        public EscoRefDto(String uri) {
            this(uri, null, null);
        }

        /**
         * Creates an EscoRefDto with URI and title.
         *
         * @param uri   ESCO persistent URI
         * @param title Human-readable label
         */
        public EscoRefDto(String uri, String title) {
            this(uri, title, null);
        }
    }

    // ========== Builder Pattern ==========

    /**
     * Creates a new builder for StandardCodesDto.
     *
     * @return new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for StandardCodesDto for fluent construction.
     */
    public static class Builder {
        private BigFiveRefDto bigFiveRef;
        private OnetRefDto onetRef;
        private EscoRefDto escoRef;

        public Builder bigFiveRef(BigFiveRefDto bigFiveRef) {
            this.bigFiveRef = bigFiveRef;
            return this;
        }

        /**
         * Sets the Big Five reference with trait, title and facet.
         *
         * @param trait Big Five dimension (OPENNESS, CONSCIENTIOUSNESS, etc.)
         * @param title Human-readable display name
         * @param facet Optional sub-facet
         */
        public Builder bigFiveRef(String trait, String title, String facet) {
            this.bigFiveRef = new BigFiveRefDto(trait, title, facet);
            return this;
        }

        /**
         * Sets the Big Five reference with only the trait.
         *
         * @param trait Big Five dimension
         */
        public Builder bigFive(String trait) {
            this.bigFiveRef = new BigFiveRefDto(trait);
            return this;
        }

        /**
         * Sets the Big Five reference with trait and title.
         *
         * @param trait Big Five dimension
         * @param title Human-readable display name
         */
        public Builder bigFive(String trait, String title) {
            this.bigFiveRef = new BigFiveRefDto(trait, title);
            return this;
        }

        public Builder onetRef(OnetRefDto onetRef) {
            this.onetRef = onetRef;
            return this;
        }

        public Builder onetRef(String code, String title, String elementType) {
            this.onetRef = new OnetRefDto(code, title, elementType);
            return this;
        }

        public Builder escoRef(EscoRefDto escoRef) {
            this.escoRef = escoRef;
            return this;
        }

        public Builder escoRef(String uri, String title, String skillType) {
            this.escoRef = new EscoRefDto(uri, title, skillType);
            return this;
        }

        public StandardCodesDto build() {
            return new StandardCodesDto(bigFiveRef, onetRef, escoRef);
        }
    }
}
