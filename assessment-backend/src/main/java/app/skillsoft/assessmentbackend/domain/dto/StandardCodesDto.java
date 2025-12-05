package app.skillsoft.assessmentbackend.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * DTO for standard occupational and skill taxonomy mappings.
 * <p>
 * Provides type-safe, validated container for external standard references
 * including O*NET occupational codes and ESCO skill/competence URIs.
 * </p>
 *
 * <h3>Supported Standards:</h3>
 * <ul>
 *   <li><strong>O*NET</strong>: US Department of Labor's Occupational Information Network</li>
 *   <li><strong>ESCO</strong>: European Skills, Competences, Qualifications and Occupations</li>
 *   <li><strong>Global Category</strong>: High-level skill classification (e.g., Big Five personality traits)</li>
 * </ul>
 *
 * <h3>Example JSON Structure:</h3>
 * <pre>{@code
 * {
 *   "global_category": {
 *     "domain": "big_five",
 *     "trait": "conscientiousness",
 *     "facet": "achievement_striving"
 *   },
 *   "onet_ref": {
 *     "code": "2.B.1.a",
 *     "title": "Oral Comprehension",
 *     "element_type": "ability"
 *   },
 *   "esco_ref": {
 *     "uri": "http://data.europa.eu/esco/skill/abc123",
 *     "title": "Communication Skills",
 *     "skill_type": "skill"
 *   }
 * }
 * }</pre>
 *
 * @see OnetRefDto
 * @see EscoRefDto
 * @see GlobalCategoryDto
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record StandardCodesDto(
        @Valid
        GlobalCategoryDto globalCategory,

        @Valid
        OnetRefDto onetRef,

        @Valid
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
        return globalCategory != null || onetRef != null || escoRef != null;
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
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
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

    /**
     * Global category DTO for high-level skill classification.
     * <p>
     * Supports various psychological and competency frameworks:
     * </p>
     * <ul>
     *   <li><strong>Big Five</strong>: Personality trait classification (OCEAN model)</li>
     *   <li><strong>Cognitive</strong>: Cognitive ability categories</li>
     *   <li><strong>Technical</strong>: Technical skill domains</li>
     * </ul>
     *
     * <h3>Example JSON (Big Five):</h3>
     * <pre>{@code
     * {
     *   "big_five": "CONSCIENTIOUSNESS",
     *   "dimension": "achievement_striving"
     * }
     * }</pre>
     *
     * <h3>Example JSON (Legacy format):</h3>
     * <pre>{@code
     * {
     *   "domain": "big_five",
     *   "trait": "conscientiousness",
     *   "facet": "achievement_striving"
     * }
     * }</pre>
     *
     * @param bigFive   Big Five personality dimension (OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, EMOTIONAL_STABILITY)
     * @param dimension Optional sub-facet/dimension of the Big Five trait (e.g., "self_discipline", "achievement_striving")
     * @param domain    Legacy: Primary classification domain (e.g., "big_five", "cognitive", "technical")
     * @param trait     Legacy: Specific trait within the domain (e.g., "conscientiousness", "extraversion")
     * @param facet     Legacy: Optional sub-facet of the trait (e.g., "achievement_striving", "self_discipline")
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record GlobalCategoryDto(
            @Pattern(
                    regexp = "^(OPENNESS|CONSCIENTIOUSNESS|EXTRAVERSION|AGREEABLENESS|EMOTIONAL_STABILITY)$",
                    message = "Big Five must be one of: OPENNESS, CONSCIENTIOUSNESS, EXTRAVERSION, AGREEABLENESS, EMOTIONAL_STABILITY"
            )
            String bigFive,

            @Size(max = 100, message = "Dimension must not exceed 100 characters")
            @Pattern(
                    regexp = "^[a-z][a-z0-9_]*$",
                    message = "Dimension must be lowercase with underscores (snake_case)"
            )
            String dimension,

            // Legacy fields for backwards compatibility
            @Pattern(
                    regexp = "^(big_five|cognitive|technical|interpersonal|leadership|emotional_intelligence)$",
                    message = "Domain must be one of: big_five, cognitive, technical, interpersonal, leadership, emotional_intelligence"
            )
            String domain,

            @Pattern(
                    regexp = "^(openness|conscientiousness|extraversion|agreeableness|neuroticism|emotional_stability|" +
                            "analytical|verbal|numerical|spatial|memory|processing_speed|" +
                            "programming|data_analysis|system_design|cloud|security|" +
                            "communication|collaboration|conflict_resolution|" +
                            "strategic_thinking|decision_making|delegation|motivation|" +
                            "self_awareness|self_regulation|empathy|social_skills)$",
                    message = "Invalid trait value for the specified domain"
            )
            String trait,

            @Size(max = 100, message = "Facet must not exceed 100 characters")
            @Pattern(
                    regexp = "^[a-z][a-z0-9_]*$",
                    message = "Facet must be lowercase with underscores (snake_case)"
            )
            String facet
    ) implements Serializable {

        /**
         * Creates a GlobalCategoryDto with only Big Five dimension.
         *
         * @param bigFive Big Five personality dimension
         */
        public GlobalCategoryDto(String bigFive) {
            this(bigFive, null, null, null, null);
        }

        /**
         * Creates a GlobalCategoryDto with Big Five dimension and sub-facet.
         *
         * @param bigFive   Big Five personality dimension
         * @param dimension Sub-facet of the trait
         */
        public GlobalCategoryDto(String bigFive, String dimension) {
            this(bigFive, dimension, null, null, null);
        }

        /**
         * Creates a legacy GlobalCategoryDto with domain, trait, and facet.
         * Use for backwards compatibility with existing data.
         *
         * @param domain Primary classification domain
         * @param trait  Specific trait within the domain
         * @param facet  Optional sub-facet of the trait
         * @return new GlobalCategoryDto using legacy format
         */
        public static GlobalCategoryDto legacy(String domain, String trait, String facet) {
            return new GlobalCategoryDto(null, null, domain, trait, facet);
        }

        /**
         * Checks if this category represents a Big Five personality trait.
         *
         * @return true if bigFive is set or domain is "big_five"
         */
        @JsonIgnore
        public boolean isBigFive() {
            return bigFive != null || "big_five".equals(domain);
        }

        /**
         * Gets the effective Big Five dimension, handling both new and legacy formats.
         *
         * @return Big Five dimension string or null
         */
        @JsonIgnore
        public String getEffectiveBigFive() {
            if (bigFive != null) {
                return bigFive;
            }
            if ("big_five".equals(domain) && trait != null) {
                return trait.toUpperCase();
            }
            return null;
        }

        /**
         * Gets the effective dimension/facet, handling both new and legacy formats.
         *
         * @return dimension or facet string, or null
         */
        @JsonIgnore
        public String getEffectiveDimension() {
            if (dimension != null) {
                return dimension;
            }
            return facet;
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
        private GlobalCategoryDto globalCategory;
        private OnetRefDto onetRef;
        private EscoRefDto escoRef;

        public Builder globalCategory(GlobalCategoryDto globalCategory) {
            this.globalCategory = globalCategory;
            return this;
        }

        /**
         * Sets the global category using the new Big Five format.
         *
         * @param bigFive   Big Five dimension (OPENNESS, CONSCIENTIOUSNESS, etc.)
         * @param dimension Optional sub-facet
         */
        public Builder bigFive(String bigFive, String dimension) {
            this.globalCategory = new GlobalCategoryDto(bigFive, dimension);
            return this;
        }

        /**
         * Sets the global category using the new Big Five format without dimension.
         *
         * @param bigFive Big Five dimension
         */
        public Builder bigFive(String bigFive) {
            this.globalCategory = new GlobalCategoryDto(bigFive);
            return this;
        }

        /**
         * Sets the global category using the legacy domain/trait/facet format.
         *
         * @param domain Primary classification domain
         * @param trait  Specific trait within the domain
         * @param facet  Optional sub-facet of the trait
         */
        public Builder globalCategory(String domain, String trait, String facet) {
            this.globalCategory = GlobalCategoryDto.legacy(domain, trait, facet);
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
            return new StandardCodesDto(globalCategory, onetRef, escoRef);
        }
    }
}
