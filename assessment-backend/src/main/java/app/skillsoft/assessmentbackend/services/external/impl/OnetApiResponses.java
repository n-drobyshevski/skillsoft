package app.skillsoft.assessmentbackend.services.external.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Package-private DTOs for deserializing O*NET Web Services API responses.
 *
 * <p>API documentation: https://services.onetcenter.org/reference/</p>
 */
final class OnetApiResponses {

    private OnetApiResponses() {
        // utility class
    }

    /**
     * Wrapper for GET /online/occupations/{code} — occupation is nested under "occupation" key.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OccupationWrapper(
            OccupationDetail occupation
    ) {}

    /**
     * Occupation detail fields: code, title, description.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OccupationDetail(
            String code,
            String title,
            String description
    ) {}

    /**
     * Response for skills/abilities/knowledge endpoints — elements under "element" key.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CategoryResponse(
            List<ElementScore> element
    ) {}

    /**
     * Individual element with id, name, and score.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ElementScore(
            String id,
            String name,
            ScoreDetail score
    ) {}

    /**
     * Score detail with numeric value and scale identifier.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record ScoreDetail(
            double value,
            @JsonProperty("scale_id")
            String scaleId
    ) {}

    /**
     * Response for GET /online/search — occupations under "occupation" key.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(
            List<SearchOccupation> occupation
    ) {}

    /**
     * Lightweight occupation result from search.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchOccupation(
            String code,
            String title
    ) {}
}
